package org.icepdf.core.pobjects.fonts.nfont.lang;

import java.util.EmptyStackException;

/**
 * Simple array based stack that holds int primitives.
 *
 * @since 4.5
 */
public class Stack {

    public int offset = 0;

    private int[] stack = new int[25];


    public int size() {
        return offset;
    }

    public void push(int value) {
        stack[offset] = value;
        offset++;
        // at max capacity increase the size of the array
        if (offset == stack.length) {
            int[] tmp = new int[(int) (offset * 1.5)];
            System.arraycopy(stack, 0, tmp, 0, stack.length);
            stack = tmp;
        }
    }

    public int pop() {
        offset--;
        if (offset < 0) {
            throw new EmptyStackException();
        }
        return stack[offset];
    }

    public void removeAllElements() {
        offset = 0;
    }

    public boolean isEmpty() {
        return offset <= 0;
    }

    public int elementAt(int index) {
        return stack[offset - index];
    }

    public int remove(int index) {
        index = offset - index;
        int[] tmp = new int[stack.length];
        int item = stack[index];
        // copy up to the index
        System.arraycopy(stack, 0, tmp, 0, index);
        // copy skipping the index
        System.arraycopy(stack, index + 1, tmp, index, stack.length - index - 1);
        offset--;
        stack = tmp;
        return item;
    }

    public void removeElement(int index) {
        remove(index);
    }

    /**
     * Format and print out the content of the stack in a direction, without changing the
     * stack pointer.
     * @param count  N to print out N stack contents in shallower (pop) direction. -N to
     * print out stack contents in deeper (push) direction
     * @return  formatted string containing values
     */
    public String getDebug(int count) {

        int myCount = Math.abs(count);
        StringBuilder sb = new StringBuilder();
        for (int idx = 0; idx < myCount; idx ++) {
            sb.append(" ");
            if (count > 0) {
                sb.append( Integer.toString( offset + idx)).append("->").append( Integer.toString( stack[offset + idx]));
            } else {
                sb.append( Integer.toString(offset - (idx+1))).append("->").append(Integer.toString( stack[offset - (idx+1) ]));
            }
        }
        return sb.toString();
    }

    public String getBriefDebug() {
        if (offset > 0) {
            return " - stackPtr:"+ (offset-1) + " -> [" + stack[offset-1] + "]";
        } else {
            return " - stackPtr: 0 -> [" + stack[0] + "]";
        }
    }

//
//    public String getBriefDebug() {
//        return " stackPtr:"+ (offset-1) + "->[" + stack[offset-1] + "]";
//    }
}
