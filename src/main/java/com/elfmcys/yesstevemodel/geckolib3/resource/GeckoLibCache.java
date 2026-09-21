package com.elfmcys.yesstevemodel.geckolib3.resource;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.MolangParser;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.molang.parser.ParseException;
// 1.12.2：绑定类深绑 1.17+ 实体/compat 面（YSMBinding 627 行/CtrlBinding 231 行/
// QueryBinding 12 个 MC import），<1.14 走精简注册面（legacy-1222-l2-full）
//? if <1.14 {
import com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.MathBinding;
//? }
//? if >=1.14 {
import com.elfmcys.yesstevemodel.client.animation.molang.TLMBinding;
import com.elfmcys.yesstevemodel.client.animation.molang.YSMBinding;
import com.elfmcys.yesstevemodel.client.animation.molang.ArgsVariable;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.MathBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.QueryBinding;
import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.animation.molang.FnBinding;
import org.apache.commons.lang3.concurrent.ConcurrentException;
//? }

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

public class GeckoLibCache {

    private static final ConcurrentLinkedQueue<MolangParser> PARSER_POOL = new ConcurrentLinkedQueue<>();

    private static final Map<String, Object> EXTRA_BINDING = new HashMap<>();

    private static final Map<String, Object> bindings = new HashMap<>();

    private static final Pattern ROAMING_VAR_PATTERN = Pattern.compile("^([;\\s]*(v|variable)\\.roaming\\.[A-Za-z0-9_]+\\s*=[^;]+[;\\s]*)+$", 2);

    public static MolangParser getMolangParser() {
        MolangParser parser = PARSER_POOL.poll();
        if (parser == null) {
            return createMolangParser();
        }
        return parser;
    }

    public static void releaseParser(MolangParser parser) {
        parser.reset();
        PARSER_POOL.add(parser);
    }

    public static IValue parseSimpleExpression(String molangExpression) throws ParseException {
        MolangParser parser = getMolangParser();
        try {
            return parser.parseExpressionUnsafe(molangExpression, false);
        } finally {
            releaseParser(parser);
        }
    }

    private static MolangParser createMolangParser() {
        MolangParser parser;
        //? if <1.14 {
        if (EXTRA_BINDING.isEmpty()) {
            EXTRA_BINDING.put("math", MathBinding.INSTANCE);
        }
        HashMap<String, Object> map = new HashMap<>(EXTRA_BINDING);
        parser = new MolangParser(map);
        //?} else {
        if (EXTRA_BINDING.isEmpty()) {
            try {
                EXTRA_BINDING.put("ysm", YSMBinding.INSTANCE.get());
                EXTRA_BINDING.put("ctrl", CtrlBinding.INSTANCE.get());
                EXTRA_BINDING.put("tlm", TLMBinding.INSTANCE.get());
                EXTRA_BINDING.put("args", ArgsVariable.INSTANCE);
            } catch (ConcurrentException e) {
                throw new RuntimeException(e);
            }
        }
        HashMap<String, Object> map2 = new HashMap<>(EXTRA_BINDING);
        map2.put("fn", new FnBinding());
        parser = new MolangParser(map2);
        //?}
        return parser;
    }

    public static Map<String, Object> getGlobalBindings() {
        if (bindings.isEmpty()) {
            bindings.putAll(EXTRA_BINDING);
            //? if >=1.14 {
            bindings.put("q", QueryBinding.INSTANCE);
            //? }
        }
        return bindings;
    }

    public static boolean isRoamingVariableAssignment(String str) {
        return ROAMING_VAR_PATTERN.matcher(str).find();
    }
}