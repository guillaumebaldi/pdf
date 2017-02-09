package org.icepdf.core.util.content;


/**
 *
 */
public class OperatorFactory {

//    private static HashMap<Integer, Operator> operatorCache =
//            new HashMap<Integer, Operator>();

    @SuppressWarnings(value = "unchecked")
    public static int[] getOperator(byte ch[], int offset, int length) {

        // get the operator int value.
        try {
            return OperandNames.getType(ch, offset, length);
        } catch (Throwable e) {
            return new int[]{OperandNames.NO_OP, 0};
        }
    }

}
