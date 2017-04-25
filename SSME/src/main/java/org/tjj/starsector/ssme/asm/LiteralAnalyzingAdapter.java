/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.tjj.starsector.ssme.asm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * A {@link MethodVisitor} that keeps track of stack map frame changes between
 * {@link #visitFrame(int, int, Object[], int, Object[]) visitFrame} calls. This
 * adapter must be used with the
 * {@link org.objectweb.asm.ClassReader#EXPAND_FRAMES} option. Each
 * visit<i>X</i> instruction delegates to the next visitor in the chain, if any,
 * and then simulates the effect of this instruction on the stack map frame,
 * represented by {@link #locals} and {@link #stack}. The next visitor in the
 * chain can get the state of the stack map frame <i>before</i> each instruction
 * by reading the value of these fields in its visit<i>X</i> methods (this
 * requires a reference to the AnalyzerAdapter that is before it in the chain).
 * If this adapter is used with a class that does not contain stack map table
 * attributes (i.e., pre Java 6 classes) then this adapter may not be able to
 * compute the stack map frame for each instruction. In this case no exception
 * is thrown but the {@link #locals} and {@link #stack} fields will be null for
 * these instructions.
 * 
 * @author Eric Bruneton
 * 
 * Added support for tracking literal assignments to the stack.
 * 
 * @author TehJumpingJawa
 */
public class LiteralAnalyzingAdapter extends MethodVisitor {

    /**
     * <code>List</code> of the local variable slots for current execution
     * frame. Primitive types are represented by {@link Opcodes#TOP},
     * {@link Opcodes#INTEGER}, {@link Opcodes#FLOAT}, {@link Opcodes#LONG},
     * {@link Opcodes#DOUBLE},{@link Opcodes#NULL} or
     * {@link Opcodes#UNINITIALIZED_THIS} (long and double are represented by
     * two elements, the second one being TOP). Reference types are represented
     * by String objects (representing internal names), and uninitialized types
     * by Label objects (this label designates the NEW instruction that created
     * this uninitialized value). This field is <tt>null</tt> for unreachable
     * instructions.
     */
    public List<StackElement> locals;

    /**
     * <code>List</code> of the operand stack slots for current execution frame.
     * Primitive types are represented by {@link Opcodes#TOP},
     * {@link Opcodes#INTEGER}, {@link Opcodes#FLOAT}, {@link Opcodes#LONG},
     * {@link Opcodes#DOUBLE},{@link Opcodes#NULL} or
     * {@link Opcodes#UNINITIALIZED_THIS} (long and double are represented by
     * two elements, the second one being TOP). Reference types are represented
     * by String objects (representing internal names), and uninitialized types
     * by Label objects (this label designates the NEW instruction that created
     * this uninitialized value). This field is <tt>null</tt> for unreachable
     * instructions.
     */
    public List<StackElement> stack;
    
    /**
     * The labels that designate the next instruction to be visited. May be
     * <tt>null</tt>.
     */
    private List<Label> labels;

    /**
     * Information about uninitialized types in the current execution frame.
     * This map associates internal names to Label objects. Each label
     * designates a NEW instruction that created the currently uninitialized
     * types, and the associated internal name represents the NEW operand, i.e.
     * the final, initialized type value.
     */
    public Map<Object, Object> uninitializedTypes;

    /**
     * The maximum stack size of this method.
     */
    private int maxStack;

    /**
     * The maximum number of local variables of this method.
     */
    private int maxLocals;

    /**
     * The owner's class name.
     */
    private String owner;

    /**
     * Creates a new {@link AnalyzerAdapter}. <i>Subclasses must not use this
     * constructor</i>. Instead, they must use the
     * {@link #AnalyzerAdapter(int, String, int, String, String, MethodVisitor)}
     * version.
     * 
     * @param owner
     *            the owner's internal class name.
     * @param access
     *            the method's access flags (see {@link Opcodes}).
     * @param name
     *            the method's name.
     * @param desc
     *            the method's descriptor (see {@link Type Type}).
     * @param mv
     *            the method visitor to which this adapter delegates calls. May
     *            be <tt>null</tt>.
     * @throws IllegalStateException
     *             If a subclass calls this constructor.
     */
    public LiteralAnalyzingAdapter(final String owner, final int access,
            final String name, final String desc, final MethodVisitor mv) {
        this(Opcodes.ASM5, owner, access, name, desc, mv);
        if (getClass() != LiteralAnalyzingAdapter.class) {
            throw new IllegalStateException();
        }
    }

    /**
     * Creates a new {@link AnalyzerAdapter}.
     * 
     * @param api
     *            the ASM API version implemented by this visitor. Must be one
     *            of {@link Opcodes#ASM4} or {@link Opcodes#ASM5}.
     * @param owner
     *            the owner's internal class name.
     * @param access
     *            the method's access flags (see {@link Opcodes}).
     * @param name
     *            the method's name.
     * @param desc
     *            the method's descriptor (see {@link Type Type}).
     * @param mv
     *            the method visitor to which this adapter delegates calls. May
     *            be <tt>null</tt>.
     */
    protected LiteralAnalyzingAdapter(final int api, final String owner,
            final int access, final String name, final String desc,
            final MethodVisitor mv) {
        super(api, mv);
        this.owner = owner;
        locals = new ArrayList<>();
        stack = new ArrayList<>();
        uninitializedTypes = new HashMap<Object, Object>();

        if ((access & Opcodes.ACC_STATIC) == 0) {
            if ("<init>".equals(name)) {
                locals.add(StackElement.UNKNOWN_UNINITIALIZED_THIS);
            } else {
                locals.add(new StackElement(owner));
            }
        }
        Type[] types = Type.getArgumentTypes(desc);
        for (int i = 0; i < types.length; ++i) {
            Type type = types[i];
            switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                locals.add(StackElement.UNKNOWN_INTEGER);
                break;
            case Type.FLOAT:
                locals.add(StackElement.UNKNOWN_FLOAT);
                break;
            case Type.LONG:
                locals.add(StackElement.UNKNOWN_LONG);
                locals.add(StackElement.UNKNOWN_TOP);
                break;
            case Type.DOUBLE:
                locals.add(StackElement.UNKNOWN_DOUBLE);
                locals.add(StackElement.UNKNOWN_TOP);
                break;
            case Type.ARRAY:
                locals.add(new StackElement(types[i].getDescriptor()));
                break;
            // case Type.OBJECT:
            default:
                locals.add(new StackElement(types[i].getInternalName()));
            }
        }
        maxLocals = locals.size();
    }

    @Override
    public void visitFrame(final int type, final int nLocal,
            final Object[] local, final int nStack, final Object[] stack) {
        if (type != Opcodes.F_NEW) { // uncompressed frame
            throw new IllegalStateException(
                    "ClassReader.accept() should be called with EXPAND_FRAMES flag");
        }

        if (mv != null) {
            mv.visitFrame(type, nLocal, local, nStack, stack);
        }

        if (this.locals != null) {
            this.locals.clear();
            this.stack.clear();
        } else {
            this.locals = new ArrayList<>();
            this.stack = new ArrayList<>();
        }
        for (int i = 0; i < nLocal; ++i) {
		    Object type1 = local[i];
		    locals.add(new StackElement(type1));
		    if (type1 == Opcodes.LONG || type1 == Opcodes.DOUBLE) {
		        locals.add(StackElement.UNKNOWN_TOP);
		    }
		}
        for (int i = 0; i < nStack; ++i) {
		    Object type2 = stack[i];
		    this.stack.add(new StackElement(type2));
		    if (type2 == Opcodes.LONG || type2 == Opcodes.DOUBLE) {
		    	this.stack.add(StackElement.UNKNOWN_TOP);
		    }
		}
        maxStack = Math.max(maxStack, this.stack.size());
    }

    @Override
    public void visitInsn(final int opcode) {
        if (mv != null) {
            mv.visitInsn(opcode);
        }
        execute(opcode, 0, null, null);
        if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)
                || opcode == Opcodes.ATHROW) {
            this.locals = null;
            this.stack = null;
        }
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        if (mv != null) {
            mv.visitIntInsn(opcode, operand);
        }
        execute(opcode, operand, null, null);
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
        if (mv != null) {
            mv.visitVarInsn(opcode, var);
        }
        execute(opcode, var, null, null);
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        if (opcode == Opcodes.NEW) {
            if (labels == null) {
                Label l = new Label();
                labels = new ArrayList<Label>(3);
                labels.add(l);
                if (mv != null) {
                    mv.visitLabel(l);
                }
            }
            for (int i = 0; i < labels.size(); ++i) {
                uninitializedTypes.put(labels.get(i), type);
            }
        }
        if (mv != null) {
            mv.visitTypeInsn(opcode, type);
        }
        execute(opcode, 0, type, null);
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner,
            final String name, final String desc) {
        if (mv != null) {
            mv.visitFieldInsn(opcode, owner, name, desc);
        }
        execute(opcode, 0, desc, name);
    }

    @Deprecated
    @Override
    public void visitMethodInsn(final int opcode, final String owner,
            final String name, final String desc) {
        if (api >= Opcodes.ASM5) {
            super.visitMethodInsn(opcode, owner, name, desc);
            return;
        }
        doVisitMethodInsn(opcode, owner, name, desc,
                opcode == Opcodes.INVOKEINTERFACE);
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner,
            final String name, final String desc, final boolean itf) {
        if (api < Opcodes.ASM5) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }
        doVisitMethodInsn(opcode, owner, name, desc, itf);
    }

    private void doVisitMethodInsn(int opcode, final String owner,
            final String name, final String desc, final boolean itf) {
        if (mv != null) {
            mv.visitMethodInsn(opcode, owner, name, desc, itf);
        }
        if (this.locals == null) {
            labels = null;
            return;
        }
        pop(desc);
        if (opcode != Opcodes.INVOKESTATIC) {
            Object t = pop();
            if (opcode == Opcodes.INVOKESPECIAL && name.charAt(0) == '<') {
                Object u;
                if (t == Opcodes.UNINITIALIZED_THIS) {
                    u = this.owner;
                } else {
                    u = uninitializedTypes.get(t);
                }
                for (int i = 0; i < locals.size(); ++i) {
                    if (locals.get(i).type == t) {
                        locals.set(i, new StackElement(u));
                    }
                }
                for (int i = 0; i < stack.size(); ++i) {
                    if (stack.get(i).type == t) {
                        stack.set(i, new StackElement(u));
                    }
                }
            }
        }
        pushDesc(desc, name);
        labels = null;
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm,
            Object... bsmArgs) {
        if (mv != null) {
            mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        }
        if (this.locals == null) {
            labels = null;
            return;
        }
        pop(desc);
        pushDesc(desc, name);
        labels = null;
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
        if (mv != null) {
            mv.visitJumpInsn(opcode, label);
        }
        execute(opcode, 0, null, null);
        if (opcode == Opcodes.GOTO) {
            this.locals = null;
            this.stack = null;
        }
    }

    @Override
    public void visitLabel(final Label label) {
        if (mv != null) {
            mv.visitLabel(label);
        }
        if (labels == null) {
            labels = new ArrayList<Label>(3);
        }
        labels.add(label);
    }

    @Override
    public void visitLdcInsn(final Object cst) {
        if (mv != null) {
            mv.visitLdcInsn(cst);
        }
        if (this.locals == null) {
            labels = null;
            return;
        }
        if (cst instanceof Integer) {
            push(StackElement.create((Integer)cst));
        } else if (cst instanceof Long) {
            push(StackElement.create((Long)cst));
            push(StackElement.UNKNOWN_TOP);
        } else if (cst instanceof Float) {
            push(StackElement.create((Float)cst));
        } else if (cst instanceof Double) {
            push(StackElement.create((Double)cst));
            push(StackElement.UNKNOWN_TOP);
        } else if (cst instanceof String) {
            push(StackElement.create((String)cst));
        } else if (cst instanceof Type) {
            int sort = ((Type) cst).getSort();
            if (sort == Type.OBJECT || sort == Type.ARRAY) {
                push(StackElement.UNKNOWN_CLASS);
            } else if (sort == Type.METHOD) {
                push(StackElement.UNKNOWN_METHOD);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (cst instanceof Handle) {
            push(StackElement.UNKNOWN_HANDLE);
        } else {
            throw new IllegalArgumentException();
        }
        labels = null;
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
        if (mv != null) {
            mv.visitIincInsn(var, increment);
        }
        execute(Opcodes.IINC, var, null, null);
    }

    @Override
    public void visitTableSwitchInsn(final int min, final int max,
            final Label dflt, final Label... labels) {
        if (mv != null) {
            mv.visitTableSwitchInsn(min, max, dflt, labels);
        }
        execute(Opcodes.TABLESWITCH, 0, null, null);
        this.locals = null;
        this.stack = null;
    }

    @Override
    public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
            final Label[] labels) {
        if (mv != null) {
            mv.visitLookupSwitchInsn(dflt, keys, labels);
        }
        execute(Opcodes.LOOKUPSWITCH, 0, null, null);
        this.locals = null;
        this.stack = null;
    }

    @Override
    public void visitMultiANewArrayInsn(final String desc, final int dims) {
        if (mv != null) {
            mv.visitMultiANewArrayInsn(desc, dims);
        }
        execute(Opcodes.MULTIANEWARRAY, dims, desc, null);
    }

    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
        if (mv != null) {
            this.maxStack = Math.max(this.maxStack, maxStack);
            this.maxLocals = Math.max(this.maxLocals, maxLocals);
            mv.visitMaxs(this.maxStack, this.maxLocals);
        }
    }

    // ------------------------------------------------------------------------

    private StackElement get(final int local) {
        maxLocals = Math.max(maxLocals, local + 1);
        return local < locals.size() ? locals.get(local) : StackElement.UNKNOWN_TOP;
    }

    private void set(final int local, StackElement v) {
        maxLocals = Math.max(maxLocals, local + 1);
        while (local >= locals.size()) {
            locals.add(StackElement.UNKNOWN_TOP);
        }
        locals.set(local, v);
    }

    private void push(StackElement v) {
        stack.add(v);
        maxStack = Math.max(maxStack, stack.size());
    }

    private void pushDesc(final String desc, String name) {
        int index = desc.charAt(0) == '(' ? desc.indexOf(')') + 1 : 0;
        switch (desc.charAt(index)) {
        case 'V':
            return;
        case 'Z':
        case 'C':
        case 'B':
        case 'S':
        case 'I':
        	;        	
            push(new StackElement(Opcodes.INTEGER, name));
            return;
        case 'F':
            push(new StackElement(Opcodes.FLOAT, name));
            return;
        case 'J':
            push(new StackElement(Opcodes.FLOAT, name));
            push(new StackElement(Opcodes.TOP, name));
            return;
        case 'D':
            push(new StackElement(Opcodes.DOUBLE, name));
            push(new StackElement(Opcodes.TOP, name));
            return;
        case '[':
            if (index == 0) {
                push(new StackElement(desc, name));
            } else {
                push(new StackElement(desc.substring(index, desc.length()), name));
            }
            break;
        // case 'L':
        default:
            if (index == 0) {
                push(new StackElement(desc.substring(1, desc.length() - 1), name));
            } else {
                push(new StackElement(desc.substring(index + 1, desc.length() - 1), name));
            }
        }
    }

    private StackElement pop() {
        return stack.remove(stack.size() - 1);
    }

    private StackElement pop(final int n) {
        int size = stack.size();
        int end = size - n;
        StackElement last = null;
        for (int i = size - 1; i >= end; --i) {
            last = stack.remove(i);
        }
        return last;
    }

    private void pop(final String desc) {
        char c = desc.charAt(0);
        if (c == '(') {
            int n = 0;
            Type[] types = Type.getArgumentTypes(desc);
            for (int i = 0; i < types.length; ++i) {
                n += types[i].getSize();
            }
            pop(n);
        } else if (c == 'J' || c == 'D') {
            pop(2);
        } else {
            pop(1);
        }
    }

    private void execute(final int opcode, final int iarg, final String sarg, String name) {
        if (this.locals == null) {
            labels = null;
            return;
        }
        StackElement t1, t2, t3, t4;
        switch (opcode) {
        case Opcodes.NOP:
        	break;
        case Opcodes.INEG:
        {
        	StackElement v = pop();
        	
        	if(v.isLiteral()) {
        		v = StackElement.create(-v.getInt());
        	}
        	push(v);
        }
        break;
        case Opcodes.LNEG:
        {
        	StackElement v = pop(2);
        	
        	if(v.isLiteral()) {
        		v = StackElement.create(-v.getLong());
        	}
        	push(v);
        	push(StackElement.UNKNOWN_TOP);
        }
        break;
        case Opcodes.FNEG:
        {
        	StackElement v = pop();
        	
        	if(v.isLiteral()) {
        		v = StackElement.create(-v.getFloat());
        	}
        	push(v);
        }
        break;        	
        case Opcodes.DNEG:
        {
        	StackElement v = pop();
        	
        	if(v.isLiteral()) {
        		v = StackElement.create(-v.getDouble());
        	}
        	push(v);
        	push(StackElement.UNKNOWN_TOP);
        }
        break;        	
        case Opcodes.I2B:
        {
        	StackElement v = pop();
        	
        	if(v.isLiteral()) {
        		v = StackElement.create((int)(byte)v.getInt());
        	}
        	push(v);
        }
        break;       	
        case Opcodes.I2C:
        {
        	StackElement v = pop();
        	
        	if(v.isLiteral()) {
        		v = StackElement.create((int)(char)v.getInt());
        	}
        	push(v);
        }
        break;     	
        case Opcodes.I2S:
        {
        	StackElement v = pop();
        	
        	if(v.isLiteral()) {
        		v = StackElement.create((int)(short)v.getInt());
        	}
        	push(v);
        }
        break;         	
        case Opcodes.GOTO:
        case Opcodes.RETURN:
            break;
        case Opcodes.ACONST_NULL:
            push(StackElement.NULL);
            break;
        case Opcodes.ICONST_M1:
        	push(StackElement.I_M1);
        	break;
        case Opcodes.ICONST_0:
        	push(StackElement.I_0);
        	break;
        case Opcodes.ICONST_1:
        	push(StackElement.I_1);
        	break;
        case Opcodes.ICONST_2:
        	push(StackElement.I_2);
        	break;
        case Opcodes.ICONST_3:
        	push(StackElement.I_3);
        	break;
        case Opcodes.ICONST_4:
        	push(StackElement.I_4);
        	break;
        case Opcodes.ICONST_5:
        	push(StackElement.I_5);
        	break;
        case Opcodes.BIPUSH:
        	push(StackElement.create((int)(byte)iarg));
        	break;
        case Opcodes.SIPUSH:
        	push(StackElement.create((int)(short)iarg));
            break;
        case Opcodes.LCONST_0:
        	push(StackElement.L_0);
        	push(StackElement.UNKNOWN_TOP);
        	break;
        case Opcodes.LCONST_1:
        	push(StackElement.L_1);
        	push(StackElement.UNKNOWN_TOP);
            break;
        case Opcodes.FCONST_0:
        	push(StackElement.F_0);
            break;
        case Opcodes.FCONST_1:
        	push(StackElement.F_1);
            break;
        case Opcodes.FCONST_2:
        	push(StackElement.F_2);
            break;
        case Opcodes.DCONST_0:
        	push(StackElement.D_0);
        	push(StackElement.UNKNOWN_TOP);
            break;
        case Opcodes.DCONST_1:
        	push(StackElement.D_1);
        	push(StackElement.UNKNOWN_TOP);
            break;
        case Opcodes.ILOAD:
        case Opcodes.FLOAD:
        case Opcodes.ALOAD:
            push(get(iarg));
            break;
        case Opcodes.LLOAD:
        case Opcodes.DLOAD:
            push(get(iarg));
            push(StackElement.UNKNOWN_TOP);
            break;
        case Opcodes.IALOAD:
        case Opcodes.BALOAD:
        case Opcodes.CALOAD:
        case Opcodes.SALOAD:
            pop(2);
            push(StackElement.UNKNOWN_INTEGER);
            break;
        case Opcodes.LALOAD:
        	pop(2);
        	push(StackElement.UNKNOWN_LONG);
        	push(StackElement.UNKNOWN_TOP);
        	break;
        case Opcodes.D2L:
        {
            StackElement v = pop(2);
            if(v.isLiteral()) {
            	push(StackElement.create((long)v.getDouble()));
            }
            else {
            	push(StackElement.UNKNOWN_LONG);
            }
            push(StackElement.UNKNOWN_TOP);
            break;
        }
        case Opcodes.FALOAD:
            pop(2);
            push(StackElement.UNKNOWN_FLOAT);
            break;
        case Opcodes.DALOAD:
        	pop(2);
        	push(StackElement.UNKNOWN_DOUBLE);
        	push(StackElement.UNKNOWN_TOP);
        	break;
        case Opcodes.L2D:
        {
            StackElement v = pop(2);
            if(v.isLiteral()) {
            	push(StackElement.create((double)v.getLong()));
            }
            else {
            	push(StackElement.UNKNOWN_DOUBLE);
            }
            push(StackElement.UNKNOWN_TOP);
            break;
        }
        case Opcodes.AALOAD:
            pop(1);
            t1 = pop();
            if (t1.type instanceof String) {
                pushDesc(((String) t1.type).substring(1), name);
            } else {
                push(StackElement.UNKNOWN_OBJECT);
            }
            break;
        case Opcodes.ISTORE:
        case Opcodes.FSTORE:
        case Opcodes.ASTORE:
            t1 = pop();
            set(iarg, t1);

            // below is to check if we're overwriting the top word of a double word local.
            if (iarg > 0) {
                t2 = get(iarg - 1);
                if (t2.type == Opcodes.LONG || t2.type == Opcodes.DOUBLE) {
                    set(iarg - 1, StackElement.UNKNOWN_TOP);
                }
            }
            break;
        case Opcodes.LSTORE:
        case Opcodes.DSTORE:
            t1 = pop(2);
            set(iarg, t1);
            set(iarg + 1, StackElement.UNKNOWN_TOP);
            
            // below is to check if we're overwriting the top word of a double word local.
            if (iarg > 0) {
                t2 = get(iarg - 1);
                if (t2.type == Opcodes.LONG || t2.type == Opcodes.DOUBLE) {
                    set(iarg - 1, StackElement.UNKNOWN_TOP);
                }
            }
            break;
        case Opcodes.IASTORE:
        case Opcodes.BASTORE:
        case Opcodes.CASTORE:
        case Opcodes.SASTORE:
        case Opcodes.FASTORE:
        case Opcodes.AASTORE:
            pop(3);
            break;
        case Opcodes.LASTORE:
        case Opcodes.DASTORE:
            pop(4);
            break;
        case Opcodes.POP:
        case Opcodes.IFEQ:
        case Opcodes.IFNE:
        case Opcodes.IFLT:
        case Opcodes.IFGE:
        case Opcodes.IFGT:
        case Opcodes.IFLE:
        case Opcodes.IRETURN:
        case Opcodes.FRETURN:
        case Opcodes.ARETURN:
        case Opcodes.TABLESWITCH:
        case Opcodes.LOOKUPSWITCH:
        case Opcodes.ATHROW:
        case Opcodes.MONITORENTER:
        case Opcodes.MONITOREXIT:
        case Opcodes.IFNULL:
        case Opcodes.IFNONNULL:
            pop(1);
            break;
        case Opcodes.POP2:
        case Opcodes.IF_ICMPEQ:
        case Opcodes.IF_ICMPNE:
        case Opcodes.IF_ICMPLT:
        case Opcodes.IF_ICMPGE:
        case Opcodes.IF_ICMPGT:
        case Opcodes.IF_ICMPLE:
        case Opcodes.IF_ACMPEQ:
        case Opcodes.IF_ACMPNE:
        case Opcodes.LRETURN:
        case Opcodes.DRETURN:
            pop(2);
            break;
        case Opcodes.DUP:
            t1 = pop();
            push(t1);
            push(t1);
            break;
        case Opcodes.DUP_X1:
            t1 = pop();
            t2 = pop();
            push(t1);
            push(t2);
            push(t1);
            break;
        case Opcodes.DUP_X2:
            t1 = pop();
            t2 = pop();
            t3 = pop();
            push(t1);
            push(t3);
            push(t2);
            push(t1);
            break;
        case Opcodes.DUP2:
            t1 = pop();
            t2 = pop();
            push(t2);
            push(t1);
            push(t2);
            push(t1);
            break;
        case Opcodes.DUP2_X1:
            t1 = pop();
            t2 = pop();
            t3 = pop();
            push(t2);
            push(t1);
            push(t3);
            push(t2);
            push(t1);
            break;
        case Opcodes.DUP2_X2:
            t1 = pop();
            t2 = pop();
            t3 = pop();
            t4 = pop();
            push(t2);
            push(t1);
            push(t4);
            push(t3);
            push(t2);
            push(t1);
            break;
        case Opcodes.SWAP:
            t1 = pop();
            t2 = pop();
            push(t1);
            push(t2);
            break;
        case Opcodes.IADD:
        {
        	StackElement a = pop();
        	StackElement b = pop();
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getInt() + a.getInt()));
        	}
        	else {
        		push(StackElement.UNKNOWN_INTEGER);
        	}
        }
        break;
        case Opcodes.ISUB:
        {
        	StackElement a = pop();
        	StackElement b = pop();
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getInt() - a.getInt()));
        	}
        	else {
        		push(StackElement.UNKNOWN_INTEGER);
        	}
        }
        break;        	
        case Opcodes.IMUL:
        {
        	StackElement a = pop();
        	StackElement b = pop();
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getInt() * a.getInt()));
        	}
        	else {
        		push(StackElement.UNKNOWN_INTEGER);
        	}
        }
        break;        	
        case Opcodes.IDIV:
        {
        	StackElement a = pop();
        	StackElement b = pop();
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getInt() / a.getInt()));
        	}
        	else {
        		push(StackElement.UNKNOWN_INTEGER);
        	}
        }
        break;         	
        case Opcodes.IREM:
        {
        	StackElement a = pop();
        	StackElement b = pop();
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getInt() % a.getInt()));
        	}
        	else {
        		push(StackElement.UNKNOWN_INTEGER);
        	}
        }
        break;         	
        case Opcodes.IAND:
        {
        	StackElement a = pop();
        	StackElement b = pop();
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getInt() & a.getInt()));
        	}
        	else {
        		push(StackElement.UNKNOWN_INTEGER);
        	}
        }
        break;         	
        case Opcodes.IOR:
        {
        	StackElement a = pop();
        	StackElement b = pop();
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getInt() | a.getInt()));
        	}
        	else {
        		push(StackElement.UNKNOWN_INTEGER);
        	}
        }
        break;         	
        case Opcodes.IXOR:
        {
        	StackElement a = pop();
        	StackElement b = pop();
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getInt() ^ a.getInt()));
        	}
        	else {
        		push(StackElement.UNKNOWN_INTEGER);
        	}
        }
        break;         	
        case Opcodes.ISHL:
        {
        	StackElement a = pop();
        	StackElement b = pop();
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getInt() << a.getInt()));
        	}
        	else {
        		push(StackElement.UNKNOWN_INTEGER);
        	}
        }
        break;         	
        case Opcodes.ISHR:
        {
        	StackElement a = pop();
        	StackElement b = pop();
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getInt() >> a.getInt()));
        	}
        	else {
        		push(StackElement.UNKNOWN_INTEGER);
        	}
        }
        break;         	
        case Opcodes.IUSHR:
        {
        	StackElement a = pop();
        	StackElement b = pop();
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getInt() >> a.getInt()));
        	}
        	else {
        		push(StackElement.UNKNOWN_INTEGER);
        	}
        }
        break;         	
        case Opcodes.L2I:
        {
        	StackElement v = pop(2);
            if(v.isLiteral()) {
            	push(StackElement.create((int)v.getLong()));
            }
            else {
            	push(StackElement.UNKNOWN_INTEGER);
            }
        }
        break;
        case Opcodes.D2I:
        {
        	StackElement v = pop(2);
            if(v.isLiteral()) {
            	push(StackElement.create((int)v.getDouble()));
            }
            else {
            	push(StackElement.UNKNOWN_INTEGER);
            }
        }
        break;
        case Opcodes.FCMPL:
        {
        	StackElement ele2 = pop();
        	StackElement ele1 = pop();
        	
        	if(bothKnown(ele2, ele1)) {
        		float val2 = ele2.getFloat();
        		float val1 = ele1.getFloat();
        		
        		if(val1>val2) {
        			push(StackElement.I_1);
        		}
        		else if(val2==val1) {
            		push(StackElement.I_0);
            	}
            	else if(val1<val2) {
            		push(StackElement.I_M1);
            	}
        		else {
        			// one, or both are NaN.
        			push(StackElement.I_M1);        			
        		}
        	}
        	else {
                push(StackElement.UNKNOWN_INTEGER);
        	}
        }
    	break;
        case Opcodes.FCMPG:
        {
        	StackElement ele2 = pop();
        	StackElement ele1 = pop();
        	
        	if(bothKnown(ele2, ele1)) {
        		float val2 = ele2.getFloat();
        		float val1 = ele1.getFloat();
        		
        		if(val1>val2) {
        			push(StackElement.I_1);
        		}
        		else if(val2==val1) {
            		push(StackElement.I_0);
            	}
            	else if(val1<val2) {
            		push(StackElement.I_M1);
            	}
        		else {
        			// one, or both are NaN.
        			push(StackElement.I_1);        			
        		}
        	}
        	else {
                push(StackElement.UNKNOWN_INTEGER);
        	}
        }
    	break;
        case Opcodes.LADD:
        {
        	StackElement a = pop(2);
        	StackElement b = pop(2);
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getLong() + a.getLong()));
        	}
        	else {
        		push(StackElement.UNKNOWN_LONG);
        	}
        	push(StackElement.UNKNOWN_TOP);
        }
        break;         	
        case Opcodes.LSUB:
        {
        	StackElement a = pop(2);
        	StackElement b = pop(2);
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getLong() - a.getLong()));
        	}
        	else {
        		push(StackElement.UNKNOWN_LONG);
        	}
        	push(StackElement.UNKNOWN_TOP);
        }
        break;         	
        case Opcodes.LMUL:
        {
        	StackElement a = pop(2);
        	StackElement b = pop(2);
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getLong() * a.getLong()));
        	}
        	else {
        		push(StackElement.UNKNOWN_LONG);
        	}
        	push(StackElement.UNKNOWN_TOP);
        }
        break;         	
        case Opcodes.LDIV:
        {
        	StackElement a = pop(2);
        	StackElement b = pop(2);
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create( b.getLong() / a.getLong()));
        	}
        	else {
        		push(StackElement.UNKNOWN_LONG);
        	}
        	push(StackElement.UNKNOWN_TOP);
        }
        break;         	
        case Opcodes.LREM:
        {
        	StackElement a = pop(2);
        	StackElement b = pop(2);
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getLong() % a.getLong()));
        	}
        	else {
        		push(StackElement.UNKNOWN_LONG);
        	}
        	push(StackElement.UNKNOWN_TOP);
        }
        break;         	
        case Opcodes.LAND:
        {
        	StackElement a = pop(2);
        	StackElement b = pop(2);
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getLong() & a.getLong()));
        	}
        	else {
        		push(StackElement.UNKNOWN_LONG);
        	}
        	push(StackElement.UNKNOWN_TOP);
        }
        break;         	
        case Opcodes.LOR:
        {
        	StackElement a = pop(2);
        	StackElement b = pop(2);
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getLong() | a.getLong()));
        	}
        	else {
        		push(StackElement.UNKNOWN_LONG);
        	}
        	push(StackElement.UNKNOWN_TOP);
        }
        break; 
        case Opcodes.LXOR:
        {
        	StackElement a = pop(2);
        	StackElement b = pop(2);
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getLong() ^ a.getLong()));
        	}
        	else {
        		push(StackElement.UNKNOWN_LONG);
        	}
        	push(StackElement.UNKNOWN_TOP);
        }
        break;         	
        case Opcodes.FADD:
        {
        	StackElement a = pop();
        	StackElement b = pop();
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getFloat() + a.getFloat()));
        	}
        	else {
        		push(StackElement.UNKNOWN_FLOAT);
        	}
        }
        break;         	
        case Opcodes.FSUB:
        {
        	StackElement a = pop();
        	StackElement b = pop();
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getFloat() - a.getFloat()));
        	}
        	else {
        		push(StackElement.UNKNOWN_FLOAT);
        	}
        }
        break;         	
        case Opcodes.FMUL:
        {
        	StackElement a = pop();
        	StackElement b = pop();
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getFloat() * a.getFloat()));
        	}
        	else {
        		push(StackElement.UNKNOWN_FLOAT);
        	}
        }
        break;         	
        case Opcodes.FDIV:
        {
        	StackElement a = pop();
        	StackElement b = pop();
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getFloat() / a.getFloat()));
        	}
        	else {
        		push(StackElement.UNKNOWN_FLOAT);
        	}
        }
        break;         	
        case Opcodes.FREM:
        {
        	StackElement a = pop();
        	StackElement b = pop();
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getFloat() % a.getFloat()));
        	}
        	else {
        		push(StackElement.UNKNOWN_FLOAT);
        	}
        }
        break;         	
        case Opcodes.L2F:
        {
        	StackElement v = pop(2);
            if(v.isLiteral()) {
            	push(StackElement.create((float)v.getLong()));
            }
            else {
            	push(StackElement.UNKNOWN_FLOAT);
            }
        }
        break;        	
        case Opcodes.D2F:
        {
        	StackElement v = pop(2);
            if(v.isLiteral()) {
            	push(StackElement.create((float)v.getDouble()));
            }
            else {
            	push(StackElement.UNKNOWN_FLOAT);
            }
        }
        break;
        case Opcodes.DADD:
        {
        	StackElement a = pop(2);
        	StackElement b = pop(2);
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getDouble() + a.getDouble()));
        	}
        	else {
        		push(StackElement.UNKNOWN_DOUBLE);
        	}
        	push(StackElement.UNKNOWN_TOP);
        }
        break;        	
        case Opcodes.DSUB:
        {
        	StackElement a = pop(2);
        	StackElement b = pop(2);
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getDouble() - a.getDouble()));
        	}
        	else {
        		push(StackElement.UNKNOWN_DOUBLE);
        	}
        	push(StackElement.UNKNOWN_TOP);
        }
        break;          	
        case Opcodes.DMUL:
        {
        	StackElement a = pop(2);
        	StackElement b = pop(2);
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getDouble() * a.getDouble()));
        	}
        	else {
        		push(StackElement.UNKNOWN_DOUBLE);
        	}
        	push(StackElement.UNKNOWN_TOP);
        }
        break;          	
        case Opcodes.DDIV:
        {
        	StackElement a = pop(2);
        	StackElement b = pop(2);
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getDouble() / a.getDouble()));
        	}
        	else {
        		push(StackElement.UNKNOWN_DOUBLE);
        	}
        	push(StackElement.UNKNOWN_TOP);
        }
        break;          	
        case Opcodes.DREM:
        {
        	StackElement a = pop(2);
        	StackElement b = pop(2);
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getDouble() % a.getDouble()));
        	}
        	else {
        		push(StackElement.UNKNOWN_DOUBLE);
        	}
        	push(StackElement.UNKNOWN_TOP);
        }
        break;  
        case Opcodes.LSHL:
        {
        	StackElement a = pop();
        	StackElement b = pop(2);
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getLong() << a.getInt()));
        	}
        	else {
        		push(StackElement.UNKNOWN_LONG);
        	}
        	push(StackElement.UNKNOWN_TOP);
        }
        break;
        case Opcodes.LSHR:
        {
        	StackElement a = pop();
        	StackElement b = pop(2);
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getLong() >> a.getInt()));
        	}
        	else {
        		push(StackElement.UNKNOWN_LONG);
        	}
        	push(StackElement.UNKNOWN_TOP);
        }
        break;        	
        case Opcodes.LUSHR:
        {
        	StackElement a = pop();
        	StackElement b = pop(2);
        	
        	if(bothKnown(a, b)) {
        		push(StackElement.create(b.getLong() >>> a.getInt()));
        	}
        	else {
        		push(StackElement.UNKNOWN_LONG);
        	}
        	push(StackElement.UNKNOWN_TOP);
        }
        break;
        case Opcodes.IINC:
        	
        	int index = iarg&0xFFFF;
        	int inc = iarg>>16;
        	
        	StackElement ele = get(index);
        	
        	if(ele.isLiteral()) {
        		int val = ele.getInt();
        		
        		set(index, StackElement.create(val+inc));
        	}
        	else {
        		// this isn't strictly necessary, as it's guaranteed to be an integer already
        		set(index, StackElement.UNKNOWN_INTEGER);
        	}
            break;
        case Opcodes.I2L:
        {
        	StackElement v = pop();
            if(v.isLiteral()) {
            	push(StackElement.create((long)v.getInt()));
            }
            else {
            	push(StackElement.UNKNOWN_LONG);
            }
            push(StackElement.UNKNOWN_TOP);
        }
        break;        	
        case Opcodes.F2L:
        {
        	StackElement v = pop();
            if(v.isLiteral()) {
            	push(StackElement.create((long)v.getFloat()));
            }
            else {
            	push(StackElement.UNKNOWN_LONG);
            }
            push(StackElement.UNKNOWN_TOP);
        }
        break;
        case Opcodes.I2F:
        {
        	StackElement v = pop();
            if(v.isLiteral()) {
            	push(StackElement.create((float)v.getInt()));
            }
            else {
            	push(StackElement.UNKNOWN_FLOAT);
            }
        }
        break;
        case Opcodes.I2D:
        {
        	StackElement v = pop();
            if(v.isLiteral()) {
            	push(StackElement.create((double)v.getInt()));
            }
            else {
            	push(StackElement.UNKNOWN_DOUBLE);
            }
            push(StackElement.UNKNOWN_TOP);
        }
        break;          	
        case Opcodes.F2D:
        {
        	StackElement v = pop();
            if(v.isLiteral()) {
            	push(StackElement.create((double)v.getFloat()));
            }
            else {
            	push(StackElement.UNKNOWN_DOUBLE);
            }
            push(StackElement.UNKNOWN_TOP);
        }
        break; 
        case Opcodes.F2I:
        {
        	StackElement v = pop();
            if(v.isLiteral()) {
            	push(StackElement.create((int)v.getFloat()));
            }
            else {
            	push(StackElement.UNKNOWN_INTEGER);
            }
        }
        break;         	
        case Opcodes.ARRAYLENGTH:
        case Opcodes.INSTANCEOF:
            pop(1);
            push(StackElement.UNKNOWN_INTEGER);
            break;
        case Opcodes.LCMP:
        {
        	StackElement ele2 = pop(2);
        	StackElement ele1 = pop(2);
        	
        	if(bothKnown(ele2, ele1)) {
        		long val2 = ele2.getLong();
        		long val1 = ele1.getLong();
        		
        		if(val2==val1) {
        			push(StackElement.I_0);
        		}
        		else if(val1>val2) {
        			push(StackElement.I_1);
        		}
        		else {
        			push(StackElement.I_M1);
        		}
        	}
        	else {
                push(StackElement.UNKNOWN_INTEGER);
        	}
        }
    	break;
        case Opcodes.DCMPL:
        {
        	StackElement ele2 = pop(2);
        	StackElement ele1 = pop(2);
        	
        	if(bothKnown(ele2, ele1)) {
        		double val2 = ele2.getDouble();
        		double val1 = ele1.getDouble();
        		
        		if(val1>val2) {
        			push(StackElement.I_1);
        		}
        		else if(val2==val1) {
            		push(StackElement.I_0);
            	}
            	else if(val1<val2) {
            		push(StackElement.I_M1);
            	}
        		else {
        			// one, or both are NaN.
        			push(StackElement.I_M1);        			
        		}
        	}
        	else {
                push(StackElement.UNKNOWN_INTEGER);
        	}
        }
    	break;
        case Opcodes.DCMPG:
        {
        	StackElement ele2 = pop(2);
        	StackElement ele1 = pop(2);
        	
        	if(bothKnown(ele2, ele1)) {
        		double val2 = ele2.getDouble();
        		double val1 = ele1.getDouble();
        		
        		if(val1>val2) {
        			push(StackElement.I_1);
        		}
        		else if(val2==val1) {
            		push(StackElement.I_0);
            	}
            	else if(val1<val2) {
            		push(StackElement.I_M1);
            	}
        		else {
        			// one, or both are NaN.
        			push(StackElement.I_1);        			
        		}
        	}
        	else {
                push(StackElement.UNKNOWN_INTEGER);
        	}
        }
    	break;
        case Opcodes.JSR:
        case Opcodes.RET:
            throw new RuntimeException("JSR/RET are not supported");
        case Opcodes.GETSTATIC:
            pushDesc(sarg, name);
            break;
        case Opcodes.PUTSTATIC:
            pop(sarg);
            break;
        case Opcodes.GETFIELD:
            pop(1);
            pushDesc(sarg, name);
            break;
        case Opcodes.PUTFIELD:
            pop(sarg);
            pop();
            break;
        case Opcodes.NEW:
            push(new StackElement(labels.get(0)));
            break;
        case Opcodes.NEWARRAY:
            pop();
            switch (iarg) {
            case Opcodes.T_BOOLEAN:
                pushDesc("[Z", name);
                break;
            case Opcodes.T_CHAR:
                pushDesc("[C", name);
                break;
            case Opcodes.T_BYTE:
                pushDesc("[B", name);
                break;
            case Opcodes.T_SHORT:
                pushDesc("[S", name);
                break;
            case Opcodes.T_INT:
                pushDesc("[I", name);
                break;
            case Opcodes.T_FLOAT:
                pushDesc("[F", name);
                break;
            case Opcodes.T_DOUBLE:
                pushDesc("[D", name);
                break;
            // case Opcodes.T_LONG:
            default:
                pushDesc("[J", name);
                break;
            }
            break;
        case Opcodes.ANEWARRAY:
            pop();
            pushDesc("[" + Type.getObjectType(sarg), name);
            break;
        case Opcodes.CHECKCAST:
            pop();
            pushDesc(Type.getObjectType(sarg).getDescriptor(), name);
            break;
        // case Opcodes.MULTIANEWARRAY:
        default:
            pop(iarg);
            pushDesc(sarg, name);
            break;
        }
        labels = null;
    }
    
    private static boolean bothKnown(StackElement a, StackElement b) {
    	return a.isLiteral() && b.isLiteral();
    }
}
