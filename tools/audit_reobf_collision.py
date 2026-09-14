#!/usr/bin/env python3
"""reobf 撞名审计：扫产线 jar 中「owner=我们自己的类 但成员名是 SRG 形态」的引用点。

背景（Task: prod-fix-reobf-collision）：legacy reobf（1.20.1 MDG legacyforge /
1.16.5 unimined searge 线）对调用点按名字映射成 SRG，而我们自己的类/接口声明
不参与映射。当我们的方法名与 vanilla mojmap 方法名撞名（如 IAudioPlayer.isStopped
撞 AbstractSoundInstance.isStopped=m_7801_），调用点被错映射成 SRG 名 → 运行时
NoSuchMethodError（debugger javap 实证）。

分类口径（关键，避免把正常 reobf 误报成崩溃）：
- BROKEN：owner 的本 jar 内父类型链（extends/implements 传递闭包）是「纯自有」
  （尽头只有 java.* 类）。此时 SRG 名成员在本 jar 链上无声明、运行时 JVM 解析只看
  owner 自身超链 → 必 NoSuchMethodError。实证案例：IAudioPlayer.m_7801_。
- OK：SRG 成员在本 jar 链上有同名同描述符声明（=我们的 vanilla 覆写被一致重命名，
  声明与调用点同步，运行期可解析）。
- INHERITED：链触达 jar 外父类型（vanilla/forge 超类），成员虽不在本 jar 声明，
  但 javac 编译时它必然存在于 owner 的编译期层级（否则编译不过）→ vanilla 层级
  内按名映射可解析（reobf 的常规正确路径）。
用法: audit_reobf_collision.py <prod.jar> [<prod2.jar> ...]
退出码: 0=无 BROKEN, 1=有 BROKEN, 2=用法/环境错误
依赖: python3 标准库 + unzip + javap（PATH 上任意 JDK8+）
"""

import re
import shutil
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

# 我们编译进产线 jar 的自有包根（含 vendored 第三方源码——它们是我们 jar 里的类，撞名风险同源）
OWN_PKGS = ("com/elfmcys/", "rip/ysm/", "org/concentus/", "org/gagravarr/", "net/sourceforge/")
# SRG 成员名形态：1.17+ 官方 tsrg（m_12345_/f_12345_，数字后下划线收尾）+
# 1.16.5 searge（func_147667_k/get_.../set_.../field_...，数字后有单字母后缀）
SRG_RE = re.compile(r"^(?:m|f|func|get|set|field)_[0-9]+_[a-z]?$")
# javap -v 引用行：Method/Field/InterfaceMethod 指令注释、BootstrapMethods 的
# REF_invoke* 注释（lambda/方法引用经 invokedynamic 走这里）。常量池裸 ref 行不扫
# （与指令注释一一重复，扫了只产生重复项）。
REF_RE = re.compile(
    r"// (?:(?:Interface)?Method|Field|REF_invoke\w+)\s+((?:%s)[A-Za-z0-9_$/]+)\.((?:m|f|func|get|set|field)_[0-9]+_[a-z]?):(\S*)"
    % "|".join(re.escape(p) for p in OWN_PKGS)
)


def parse_classfile(data: bytes):
    """解析 classfile 头部+字段/方法表：返回 (super_name, interface_names, method_set, field_set)。
    名称均为二进制斜杠形态。参考 JVM 规范 §4（仅解析结构，跳过属性体）。"""
    u1 = lambda b, o: b[o]
    u2 = lambda b, o: int.from_bytes(b[o : o + 2], "big")
    u4 = lambda b, o: int.from_bytes(b[o : o + 4], "big")

    assert u4(data, 0) == 0xCAFEBABE, "not a classfile"
    cp_count = u2(data, 8)
    cp = {}  # index -> (tag, value)
    o = 10
    CONST_LEN = {1: None, 3: 4, 4: 4, 5: 8, 6: 8, 7: 2, 8: 2, 9: 4, 10: 4, 11: 4, 12: 4, 15: 3, 16: 2, 17: 4, 18: 4, 19: 2, 20: 4}
    i = 1
    while i < cp_count:
        tag = u1(data, o)
        o += 1
        if tag == 1:  # Utf8
            ln = u2(data, o)
            cp[i] = (tag, data[o + 2 : o + 2 + ln].decode("utf-8", "replace"))
            o += 2 + ln
        else:
            n = CONST_LEN[tag]
            assert n is not None, f"bad cp tag {tag}"
            cp[i] = (tag, u2(data, o) if n == 2 else (u2(data, o), u2(data, o + 2)))
            o += n
        if tag in (5, 6):  # long/double 占两个槽位
            i += 1
        i += 1

    def utf8(idx):
        return cp[idx][1]

    def cls_name(idx):
        return utf8(cp[idx][1]) if idx else ""

    flags = u2(data, o)
    o += 2
    this_class = cls_name(u2(data, o))
    o += 2
    super_class = cls_name(u2(data, o))
    o += 2
    iflags = u2(data, o)
    o += 2
    interfaces = [cls_name(u2(data, o + 2 * k)) for k in range(iflags)]
    o += 2 * iflags

    def members(base):
        # base 指向 count 字段；表结构：count(2) × [access(2) name(2) desc(2) attrs]
        out = set()
        count = u2(data, base)
        p = base + 2
        for _ in range(count):
            p += 2  # access
            name = utf8(u2(data, p))
            desc = utf8(u2(data, p + 2))
            p += 4
            attr_count = u2(data, p)
            p += 2
            for _ in range(attr_count):
                p += 6 + u4(data, p + 2)  # name(2)+len(4)+body
            out.add((name, desc))
        return out, p

    # classfile 顺序：fields 表在前，methods 表在后（JVM 规范 §4.1）
    fields, end1 = members(o)
    methods, _ = members(end1)
    return this_class, super_class, interfaces, methods, fields


def load_jar_index(jar: Path, work: Path):
    """解包 jar 并返回 {二进制类名: (super, interfaces, methods, fields)}。"""
    with zipfile.ZipFile(jar) as z:
        names = [n for n in z.namelist() if n.endswith(".class")]
        z.extractall(work, members=names)
    index = {}
    for p in work.rglob("*.class"):
        bin_name = p.relative_to(work).as_posix()[:-6]
        try:
            this_cls, sup, itfs, methods, fields = parse_classfile(p.read_bytes())
        except Exception as e:  # 结构异常的类仍要报出来，不能静默跳过
            print(f"  !! 类解析失败 {bin_name}: {e}", file=sys.stderr)
            continue
        index[bin_name] = (sup, itfs, methods, fields)
    return index


def chain_is_pure(index, owner: str) -> bool:
    """owner 的超类型闭包是否「纯自有」：所有路径尽头都是 java.* 类（无 vanilla 根）。"""
    seen, stack = set(), [owner]
    while stack:
        cur = stack.pop()
        if cur in seen:
            continue
        seen.add(cur)
        if cur not in index:
            if not cur.startswith("java/"):
                return False  # 触达 jar 外非 java 类 = vanilla/forge 根
            continue
        sup, itfs, _, _ = index[cur]
        if sup:
            stack.append(sup)
        stack.extend(itfs)
    return True


def declared_in_chain(index, owner: str, name: str, desc: str, kind: str) -> bool:
    seen, stack = set(), [owner]
    while stack:
        cur = stack.pop()
        if cur in seen or cur not in index:
            continue
        seen.add(cur)
        sup, itfs, methods, fields = index[cur]
        if (name, desc) in (methods if kind != "Field" else fields):
            return True
        if sup:
            stack.append(sup)
        stack.extend(itfs)
    return False


def audit_jar(jar: Path):
    print(f"== {jar} ==")
    work = Path(tempfile.mkdtemp(prefix="reobf-audit-"))
    try:
        index = load_jar_index(jar, work)
        # javap -v 一遍全量；javap 不支持 @argfile（实证），经 xargs 批传类名
        javap = subprocess.run(
            ["xargs", "-0", "javap", "-v", "-p", "-cp", work.as_posix()],
            input="\0".join(sorted(index)), capture_output=True, text=True,
        )
        if javap.returncode != 0:
            print(javap.stderr[:2000], file=sys.stderr)
            raise RuntimeError("javap 失败")

        broken, ok, inherited = [], 0, 0
        for m in REF_RE.finditer(javap.stdout):
            owner, name, sig = m.group(1), m.group(2), m.group(3)
            desc = sig if sig.startswith("(") else ""  # 字段注释冒号后是描述符简称（I/Z...）
            if not SRG_RE.match(name) or owner not in index:
                continue
            kind = "Field" if "// Field" in m.group(0) else "Method"
            if declared_in_chain(index, owner, name, desc, kind):
                ok += 1  # 我们的 vanilla 覆写被一致重命名：声明+调用点同步
            elif chain_is_pure(index, owner):
                broken.append(f"  BROKEN {owner}.{name}:{sig}")
            else:
                inherited += 1  # 链触达 vanilla 超类：常规 reobf 可解析路径
        print(f"  类: {len(index)}  一致重命名(OK): {ok}  vanilla 继承(INHERITED): {inherited}  撞名(BROKEN): {len(broken)}")
        for line in sorted(set(broken)):
            print(line)
        return len(set(broken))
    finally:
        shutil.rmtree(work, ignore_errors=True)


def main():
    if len(sys.argv) < 2:
        print(__doc__, file=sys.stderr)
        return 2
    total = 0
    for arg in sys.argv[1:]:
        jar = Path(arg)
        if not jar.is_file():
            print(f"!! 找不到 jar: {jar}", file=sys.stderr)
            total += 1
            continue
        total += audit_jar(jar)
    if total:
        print(f"FAIL: 共 {total} 处 BROKEN（owner 纯自有链上的 SRG 名引用 = 错映射，运行时 NoSuchMethodError）")
        return 1
    print("OK: 无 BROKEN 撞名引用，审计归零")
    return 0


if __name__ == "__main__":
    sys.exit(main())
