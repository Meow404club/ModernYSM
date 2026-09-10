package com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.variable;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.ResetVariable;
import com.elfmcys.yesstevemodel.molang.runtime.AssignableVariable;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.elfmcys.yesstevemodel.molang.runtime.binding.ObjectBinding;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;


@SuppressWarnings("MapOrSetKeyShouldOverrideHashCodeEquals")
public class ScopedVariableBinding implements ObjectBinding, ResetVariable {

    private final Int2ReferenceOpenHashMap<ScopedVariable> variableMap = new Int2ReferenceOpenHashMap<>();

    @Override
    public Object getProperty(String name) {
        return variableMap.computeIfAbsent(StringPool.computeIfAbsent(name), ScopedVariable::new);
    }

    @Override
    public void reset() {
        this.variableMap.clear();
    }

    private static final class ScopedVariable implements AssignableVariable {
        private final int name;

        ScopedVariable(int name) {
            this.name = name;
        }

        public int name() {
            return this.name;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object evaluate(final @NotNull ExecutionContext<?> context) {
            return ((IContext<Object>) context.entity()).scopedStorage().getScoped(name);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void assign(@NotNull ExecutionContext<?> context, Object value) {
            ((IContext<Object>) context.entity()).scopedStorage().setScoped(name, value);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ScopedVariable)) {
                return false;
            }
            ScopedVariable other = (ScopedVariable) obj;
            return this.name == other.name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name);
        }

        @Override
        public String toString() {
            return "ScopedVariable[name=" + this.name + "]";
        }
    }
}