package com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.variable;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.molang.runtime.AssignableVariable;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.elfmcys.yesstevemodel.molang.runtime.binding.ObjectBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.CloseVariable;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TempVariableRegistry implements ObjectBinding, CloseVariable {

    private final Object2ReferenceMap<String, TempVariable> variableMap = new Object2ReferenceOpenHashMap();

    private int topPointer = 0;

    @Override
    public Object getProperty(String str) {
        return this.variableMap.computeIfAbsent(str, obj -> {
            int i = this.topPointer;
            this.topPointer = i + 1;
            return new TempVariable(i);
        });
    }

    @Override
    public void dispose() {
        this.variableMap.clear();
        this.topPointer = 0;
    }

    private static final class TempVariable implements AssignableVariable {
        private final int address;

        TempVariable(int address) {
            this.address = address;
        }

        public int address() {
            return this.address;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object evaluate(@NotNull ExecutionContext<?> context) {
            return ((IContext<Object>) context.entity()).tempStorage().getElement(this.address);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void assign(@NotNull ExecutionContext<?> context, Object value) {
            ((IContext<Object>) context.entity()).tempStorage().setElement(this.address, value);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof TempVariable)) {
                return false;
            }
            TempVariable other = (TempVariable) obj;
            return this.address == other.address;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.address);
        }

        @Override
        public String toString() {
            return "TempVariable[address=" + this.address + "]";
        }
    }
}