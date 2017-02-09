package org.icepdf.core.pobjects.fonts.nfont.instructions;

/**
 * This class list by opt code the entire TrueType instruction set. Definitions
 * are defined as per the specification,
 * https://developer.apple.com/fonts/TTRefMan/RM05/Chap5.html.
 * <p/>
 * Instruction names can be used to reference the corresponding instruction
 * command via Instructions.INSTRUCTIONS array
 *
 * @since 4.5
 */
public class InstructionNames {

    /**
     * AA[] Adjust Angle
     * <p/>
     * Code Range 0x7F  <br />
     * Pops p: point number (uint32) <br />
     * Pushes -
     * Related instructions	 SANGW[ ] <br />
     * <p/>
     * Pops one argument from the stack. This instruction is anachronistic
     * and has no other effect.
     */
    public static final int AA = 0x7F;


    /**
     * ABS[] ABSolute value
     * <p/>
     * Code Range 0x64<br />
     * Pops n: fixed point number (F26Dot6)<br />
     * Pushes |n|: absolute value of n (F26Dot6)<br />
     * Replaces the number at the top of the stack with its absolute value.<br />
     * <p/>
     * Pops a 26.6 fixed point number, n, off the stack and pushes the absolute
     * value of n onto the stack.
     */
    public static final int ABS = 0x64;

    /**
     * ADD[] ADD
     * <p/>
     * Code Range 0x60 <br />
     * Pops n2: fixed point number (F26Dot6) n1: fixed point number (F26Dot6)<br />
     * Pushes sum: n1 + n2(F26Dot6)<br />
     * Adds the top two numbers on the stack.
     * <p/>
     * Pops two 26.6 fixed point numbers, n2 and n1, off the stack and pushes
     * the sum of those two numbers onto the stack.
     */
    public static final int ADD = 0x60;

    /**
     * ALIGNPTS[] ALIGN Points
     * <p/>
     * Code Range 0x27<br />
     * Pops p2: point number (uint32) p1: point number (uint32) <br />
     * Pushes - <br />
     * Uses zp0 with point p2 and zp1 with point p1, freedom vector, projection vector
     * Related instructions	 ALIGNRP[ ]
     * <p/>
     * Aligns the two points whose numbers are the top two items on the stack
     * along an axis orthogonal to the projection vector.
     * <p/>
     * Pops two point numbers,
     * p2 and p1, from the stack and makes the distance between them zero by
     * moving both points along the freedom vector to the average of their
     * projections along the projection vector.
     * <p/>
     * In the illustration below,
     * points p1 and p2 are moved along the freedom vector until the projected
     * distance between them is reduced to zero. The distance from A to B equals
     * d/2 which equals the distance from B to C. The value d/2 is one-half the
     * original projected distance between p1 and p2.
     */
    public static final int ALIGNPTS = 0X27;

    /**
     * ALIGNRP[] ALIGN to Reference Point
     * <p/>
     * Code Range 0x3C <br />
     * Pops p1, p2, , ploopvalue: point numbers (uint32) <br />
     * Pushes	 - <br />
     * Uses	 zp1 with point p and zp0 with rp0, loop, freedom vector, projection vector
     * Related instructions ALIGNPTS[ ]
     * <p/>
     * Aligns the points whose numbers are at the top of the stack with the point
     * referenced by rp0.
     * <p/>
     * Pops point numbers, p1, p2, , ploopvalue, from the stack and aligns those
     * points with the current position of rp0 by moving each point pi so that
     * the projected distance from pi to rp0 is reduced to zero. The number of
     * points aligned depends up the current setting the state variable loop.
     * <p/>
     * In the illustration below, point p is moved along the freedom vector until
     * its projected distance from rp0 is reduced to zero.
     */
    public static final int ALIGNRP = 0x3c;

    /**
     * AND[] logical AND
     * <p/>
     * Code Range 0x5A <br />
     * Pops e2: stack element (StkElt) e1: stack element (StkElt)<br />
     * Pushes (e1 and e2): logical and of e1 and e2 (uint32)<br />
     * Related instructions OR[ ]<br />
     * <p/>
     * Takes the logical and of the top two stack elements.
     * <p/>
     * Pops the top two elements, e2 and e1, from the stack and pushes the
     * result of a logical and of the two elements onto the stack. Zero is pushed
     * if either or both of the elements are FALSE (have the value zero). One is
     * pushed if both elements are TRUE (have a non-zero value).
     */
    public static final int AND = 0x5A;

    /**
     * CALL[] CALL function
     * <p/>
     * Code Range 0x2B<br />
     * Pops f: function identifier number (int32 in the range 0 through (n-1) where n is specified in the 'maxp' table)<br />
     * Pushes -<br />
     * Related instructions	 FDEF[ ], EIF[ ]<br />
     * <p/>
     * Calls the function identified by the number of the top of the stack.
     * <p/>
     * Pops a function identifier number, f, from the stack and calls the function
     * identified by f. The instructions contained in the function body will be
     * executed. When execution of the function is complete, the instruction pointer
     * will move to the next location in the instruction stream where execution of
     * instructions will resume.
     */
    public static final int CALL = 0x2b;

    /**
     * CEILING[] CEILING
     * <p/>
     * Code Range 0x67<br />
     * Pops n: fixed point number (F26Dot6)<br />
     * Pushes n : ceiling of n (F26Dot6)<br />
     * Related instructions FLOOR[ ]<br />
     * <p/>
     * Takes the ceiling of the number at the top of the stack.
     * Pops a number n from the stack and pushes n , the least integer value
     * greater than or equal to n. Note that the ceiling of n, though an integer
     * value, is expressed as 26.6 fixed point number.
     */
    public static final int CEILING = 0x67;

    /**
     * CINDEX[] Copy the INDEXed element to the top of the stack
     * <p/>
     * Code Range 0x25 <br />
     * Pops k: stack element number (int32) <br />
     * Pushes ek: kth stack element (StkElt) <br />
     * Stack before k: stack element number <br />
     * e1: stack element  <br />
     * ...       <br />
     * ek: stack element  <br />
     * Stack after ek: indexed element  <br />
     * e1: stack element <br />
     * ...            <br />
     * ek: stack element  <br />
     * Related instructions MINDEX[ ] <br />
     * <p/>
     * Copies the indexed stack element to the top of the stack.
     * <p/>
     * Pops a stack element number, k, from the stack and pushes a copy of the
     * kth stack element on the top of the stack. Since it is a copy that is
     * pushed, the kth element remains in its original position. This feature
     * is the key difference between the CINDEX[ ] and MINDEX[ ] instructions.
     * <p/>
     * A zero or negative value for k is an error.
     */
    public static final int CINDEX = 0x25;

    /**
     * CLEAR[] CLEAR the stack
     * <p/>
     * Code Range 0x22 <br />
     * Pops all the items on the stack (StkElt) <br />
     * Pushes - <br />
     * <p/>
     * Clears all elements from the stack.
     */
    public static final int CLEAR = 0x22;

    /**
     * DEBUG[] DEBUG call
     * <p/>
     * Code Range 0x4F<br />
     * Pops n: integer (uint32)<br />
     * Pushes -<br />
     * <p/>
     * Pops an integer from the stack. In non-debugging versions of the interpreter,
     * the execution of instructions will continue. In debugging versions, available
     * to font developers, an implementation dependent debugger will be invoked.
     * <p/>
     * This instruction is only for debugging purposes and should not be a part
     * of a finished font. Some implementations do not support this instruction.
     * <p/>
     * Return to Contents
     */
    public static final int DEBUG = 0x4f;

    /**
     * DELTAC1[] DELTA exception C
     * <p/>
     * Code Range 0x73<br />
     * Pops n: number of pairs of exception specifications and CVT entry numbers (uint32) <br />
     * argn, cn, argn-1,cn-1, , arg1, c1: pairs of CVT entry number and exception specifications (pairs of uint32s)  <br />
     * Pushes -<br />
     * Uses delta shift, delta base <br />
     * Related instructions	 DELTAC2[ ], DELTAC3, DELTAP1, DELTAP2, DELTAP3 <br />
     * <p/>
     * Creates an exception to one or more CVT values, each at a specified point size and by a specified amount.
     * <p/>
     * Pops an integer, n, followed by n pairs of exception specifications and control value table entry numbers. DELTAC1[] changes the value in each CVT entry specified at the size and by the pixel amount specified in its paired argument.
     * <p/>
     * The 8 bit arg component of the DELTAC1[] instruction decomposes into two parts. The most significant 4 bits represent the relative number of pixels per em at which the exception is applied. The least significant 4 bits represent the magnitude of the change to be made.
     * <p/>
     * The relative number of pixels per em is a function of the value specified in the argument and the delta base. The DELTAC1[] instruction works at pixel per em sizes beginning with the delta base through the delta_base + 15. To invoke an exception at a larger pixel per em size, use the DELTAC2[] or DELTAC3[] instruction which can affect changes at sizes up to delta_base + 47 or, if necessary, increase the value of the delta base.
     * <p/>
     * The magnitude of the move is specified, in a coded form, in the instruction. Table 5 lists the mapping from exception values and the magnitude of the move made.The size of the step depends on the value of the delta shift.
     * Table 4: Magnitude values mapped to number of steps to move
     * <p/>
     * <table>
     * <tr><td></td><td>Selector</td><td>0</td><td>1</td><td>2</td><td>3</td><td>4</td><td>5</td><td>6</td><td>7</td><td> 8</td><td>9</td><td>10</td><td>11</td><td>12</td><td>13</td><td>14</td><td>15</td>
     * </tr>
     * <tr><td></td>Number of steps</td><td>-8</td><td>-7</td><td>-6</td><td>-5</td><td>-4</td><td>-3</td><td>-2</td><td>-1</td><td>1</td><td>2</td><td>3</td><td>4</td><td>5</td><td>6</td><td>7</td><td>8</td>
     * </tr>
     * </table>
     * <p/>
     * For additional information on the DELTA instructions see Instructing Fonts .
     */
    public static final int DELTAC1 = 0x73;

    /**
     * DELTAC2[] DELTA exception C2
     * <p/>
     * Code Range 0x74 <br />
     * Pops	 n: number of pairs of exception specifications and CVT entry numbers (uint32)  <br />
     * argn, cn, argn-1,cn-1, , arg1, c1: pairs of CVT entry number and exception specifications (pairs of uint32s)  <br />
     * Pushes	 - <br />
     * Uses	 delta shift, delta base  <br />
     * Related instructions	 DELTAC2[ ], DELTAC3[], DELTAP1[], DELTAP2[], DELTAP3[]   <br />
     * <p/>
     * Creates an exception to one or more CVT values, each at a specified point
     * size and by a specified amount.
     * <p/>
     * Pops an integer, n, followed by n pairs of exception specifications and CVT
     * entry numbers. DELTAC2[] changes the value in each CVT entry specified at
     * the size and by the amount specified in its paired argument.
     * <p/>
     * The DELTAC2[] instruction is exactly the same as the DELTAC1[] instruction
     * except for operating at pixel per em sizes beginning with the (delta_base + 16) through the (delta_base + 31). To invoke an exception at a smaller pixel per em size, use the DELTAC1[] instruction. To invoke an exception at a smaller pixel per em size, use the DELTAC3[] instruction which can affect changes at sizes up to delta_base + 47 or, if necessary, change the value of the delta base.
     * <p/>
     * For more information see the entry for DELTAC1[] or Instructing Fonts .
     */
    public static final int DELTAC2 = 0x74;

    /**
     * DELTAC3[] DELTA exception C3
     * <p/>
     * Code Range	 0x75<br />
     * Pops	 n: number of pairs of CVT entry numbers and exception specifications (uint32)<br />
     * argn, cn, argn-1,cn-1, , arg1, c1: pairs of CVT entry number and exception specifications (pairs of uint32s) <br />
     * Pushes	 - <br />
     * Uses	 delta shift, delta base <br />
     * Related instructions	 DELTAC2[ ], DELTAC3[], DELTAP[], DELTAP2[], DELTAP3[] <br />
     * <p/>
     * Creates an exception to one or more CVT values, each at a specified point
     * size and by a specified amount.
     * <p/>
     * Pops an integer, n, followed by n pairs of exception specifications and
     * CVT entry numbers. DELTAC3[] changes the value in each CVT entry specified
     * at the size and by the amount specified in its paired argument.
     * <p/>
     * The DELTAC3[] instruction is exactly the same as the DELTAC1 instruction
     * except for operating at pixel per em sizes beginning with the
     * (delta_base + 32) through the (delta_base + 47).
     * <p/>
     * For more information see the entry for DELTAC1[] or Instructing Fonts .
     */
    public static final int DELTAC3 = 0x75;

    /**
     * DELTAP1[] DELTA exception P1
     * <p/>
     * Code Range 0x5D <br />
     * Pops n: number of pairs of exception specifications and points (uint32) <br />
     * argn, pn, argn-1, pn-1, , arg1, p1: n pairs of exception specifications and points (pairs of uint32s)  <br />
     * Pushes	 - <br />
     * Uses	 zp0, delta base, delta shift, freedom vector, projection vector <br />
     * Related instructions	 DELTAC2[ ], DELTAC3, DELTAP1, DELTAP2, DELTAP3 <br />
     * <p/>
     * Creates an exception at one or more point locations, each at a specified
     * point size and by a specified amount.
     * <p/>
     * DELTAP1[] works on the points in the zone reference by zp0. It moves the
     * specified points at the size and by the amount specified in the paired argument.
     * Moving a point makes it possible to turn on or off selected pixels in the bitmap that will be created when the affected outline is scan converted. An arbitrary number of points and arguments can be specified.
     * <p/>
     * The grouping [argi, pi] can be executed n times. The value of argi consists
     * of a byte with lower four bits of which represent the magnitude of the exception and the upper four bits, the relative pixel per em value.
     * <p/>
     * The actual pixel per em size at which a DELTAP instruction works is a function
     * of the relative pixel per em size and the delta base. The DELTAP1[] instruction
     * works at pixel per em sizes beginning with the delta base through the delta_base + 15.
     * To invoke an exception at a larger pixel per em size, use the DELTAP2[] or DELTAP3[]
     * instruction which together can affect changes at sizes up to delta_base + 47 or, if
     * necessary, increase the value of the delta base.
     * <p/>
     * The magnitude of the move is specified, in a coded form, in the instruction.
     * Table 5 lists the mapping from exception values used in a DELTA instruction
     * to the magnitude in steps of the move made. The size of the step depends
     * on the value of the delta shift.
     * <p/>
     * Table 5: Magnitude values mapped to number of steps to move
     * <p/>
     * <table>
     * <tr><td></td><td>Selector</td><td>0</td><td>1</td><td>2</td><td>3</td><td>4</td><td>5</td><td>6</td><td>7</td><td> 8</td><td>9</td><td>10</td><td>11</td><td>12</td><td>13</td><td>14</td><td>15</td>
     * </tr>
     * <tr><td></td>Number of steps</td><td>-8</td><td>-7</td><td>-6</td><td>-5</td><td>-4</td><td>-3</td><td>-2</td><td>-1</td><td>1</td><td>2</td><td>3</td><td>4</td><td>5</td><td>6</td><td>7</td><td>8</td>
     * </tr>
     * </table>
     * <p/>
     */
    public static final int DELTAP1 = 0x5d;

    /**
     * DELTAP2[] DELTA exception P2
     * <p/>
     * Code Range	 0x71<br />
     * Pops	 n: number of pairs of exception specifications and points (uint32)<br />
     * argn, pn, argn-1, pn-1, , arg1, p1: n pairs of exception specifications and points (pairs of uint32s)<br />
     * Pushes	 -<br />
     * Uses	 zp0, delta shift, delta base, freedom vector, projection vector<br />
     * Related instructions	 DELTAC2[ ], DELTAC3, DELTAP1, DELTAP2, DELTAP3<br />
     * <p/>
     * Creates an exception at one or more point locations, each at a specified point size and by a specified amount.
     * <p/>
     * DELTAP2[] works on the points in the zone reference by zp0. It moves the specified points at the size and by the amount specified in the paired argument. Moving a point makes it possible to turn on or off selected pixels in the bitmap that will be created when the affected outline is scan converted. An arbitrary number of points and arguments can be specified.
     * <p/>
     * The DELTAP2[] instruction is identical to the DELTAP1[] instruction save for operating at pixel per em sizes beginning with the (delta_base + 16) through the (delta_base + 31). To invoke an exception at a smaller pixel per em size, use the DELTAP1[] instruction. To invoke an exception at a smaller pixel per em size, use the DELTAP3[] instruction. If necessary, change the value of the delta_base.
     */
    public static final int DELTAP2 = 0x71;

    /**
     * DELTAP3[] DELTA exception P3
     * <p/>
     * Code Range	 0x72 <br />
     * Pops	 n: number of pairs of exception specifications and points (uint32) <br />
     * argn, pn, argn-1, pn-1, , arg1, p1: n pairs of exception specifications and points (pairs of uint32s)<br />
     * Pushes	 -   <br />
     * Uses	 zp0, delta base, delta shift, freedom vector, projection vector  <br />
     * Related instructions	 DELTAC2[ ], DELTAC3, DELTAP1, DELTAP2, DELTAP3 <br />
     * <p/>
     * Creates an exception at one or more point locations, each at a specified
     * point size and by a specified amount.
     * <p/>
     * Pops an integer, n, followed by n pairs of exception specifications and
     * points. DELTAP3[] works on the points in the zone reference by zp0. It
     * moves the specified points at the size and by the amount specified in the
     * paired argument. Moving a point makes it possible to turn on or off selected
     * pixels in the bitmap that will be created when the affected outline is scan
     * converted. An arbitrary number of points and arguments can be specified.
     * <p/>
     * The DELTAP3[] instruction is identical to the DELTAP1[] instruction save
     * for operating at pixel per em sizes beginning with the (delta_base + 32)
     * through the (delta base + 47). To invoke an exception at a smaller pixel
     * per em size, use the DELTAP1[] or the DELTAP2[] instruction. If necessary,
     * change the value of the delta base.
     */
    public static final int DELTAP3 = 0x72;

    /**
     * DEPTH[] DEPTH of the stack
     * <p/>
     * Code Range	 0x24 <br />
     * Pops	 -  <br />
     * Pushes	 n: number of elements (int32)  <br />
     * <p/>
     * Pushes n, the number of elements currently in the stack, onto the stack.
     */
    public static final int DEPTH = 0x24;

    /**
     * DIV[] DIVide
     * <p/>
     * Code Range	 0x62 <br />
     * Pops	 n2: divisor (F26Dot6)   <br />
     * n1: dividend (F26Dot6)     <br />
     * Pushes	 (n1 * 64)/n2: quotient (F26Dot6)  <br />
     * <p/>
     * Divides the number second from the top of the stack by the number at the
     * top of the stack.
     * Pops two 26.6 fixed point numbers, n1 and n2 off the stack and pushes onto
     * the stack the quotient obtained by dividing n2 by n1. The division takes
     * place in the following fashion, n1 is shifted left by six bits and then
     * divided by 2.
     */
    public static final int DIV = 0x62;

    /**
     * DUP[] DUPlicate top stack element
     * <p/>
     * Code Range	 0x20 <br />
     * Pops	 e: stack element (StkElt)    <br />
     * Pushes	 e: stack element (StkElt)  <br />
     * e: stack element (StkElt)<br />
     * <p/>
     * Duplicates the top element on the stack.
     * <p/>
     * Pops an element, e, from the stack, duplicates that element and pushes it twice.
     */
    public static final int DUP = 0x20;

    /**
     * EIF[] End IF
     * <p/>
     * Code Range	 0x59<br />
     * Pops	 -  <br />
     * Pushes	 -  <br />
     * Related instructions	 IF[ ], ELSE[ ] <br />
     * <p/>
     * Marks the end of an IF or IF-ELSE instruction sequence.
     */
    public static final int EIF = 0x59;

    /**
     * ELSE[] ELSE clause
     * <p/>
     * Code Range	 0x1B<br />
     * Pops	 -<br />
     * Pushes	 -  <br />
     * Related instructions	 IF[ ], EIF[ ]  <br />
     * <p/>
     * Marks the start of the sequence of instructions that are to be executed
     * when an IF instruction encounters a FALSE value on the stack. This sequence
     * of instructions is terminated with an EIF instruction.
     * <p/>
     * The ELSE portion of an IF-ELSE-EIF sequence is optional.
     */
    public static final int ELSE = 0x1b;

    /**
     * ENDF[] END Function definition
     * <p/>
     * Code Range	 0x2D <br />
     * Pops	 -     <br />
     * Pushes	 -    <br />
     * Related instructions	 FDEF[ ], IDEF[ ]     <br />
     * <p/>
     * Marks the end of a function definition or an instruction definition.
     * Function definitions and instruction definitions cannot be nested.
     */
    public static final int ENDF = 0x2d;

    /**
     * EQ[] EQual
     * <p/>
     * Code Range	 0x54 <br />
     * Pops	 e2: stack element e1: stack element  <br />
     * Pushes	 b: Boolean value (uint32 in the range [0,1])<br />
     * Related instructions	 NEQ[ ]  <br />
     * <p/>
     * Tests whether the top two numbers on the stack are equal in value.
     * <p/>
     * Pops two 32 bit values, e2 and e1, from the stack and compares them. If they are the same, one, signifying TRUE is pushed onto the stack. If they are not equal, zero, signifying FALSE is placed onto the stack.
     */
    public static final int EQ = 0x54;

    /**
     * EVEN[] EVEN
     * <p/>
     * Code Range	 0x57<br />
     * Pops	 e: stack element (F26Dot6)<br />
     * Pushes	 b: Boolean value (uint32 in the range [0,1])<br />
     * Uses	 round state<br />
     * Related instructions	 ODD[ ]<br />
     * <p/>
     * Tests whether the number at the top of the stack, when rounded according
     * to the round state, is even.
     * <p/>
     * Pops a 26.6 number, e, from the stack and rounds that number according to
     * the current round state. The number is then truncated to an integer. If
     * the truncated number is even, one, signifying TRUE, is pushed onto the
     * stack; if it is odd, zero, signifying FALSE, is placed onto the stack.
     */
    public static final int EVEN = 0x57;

    /**
     * FDEF[] Function DEFinition
     * <p/>
     * Code Range	 0x2C<br />
     * Pops	 f: function identifier number (integer in the range 0 through (n-1) where n is specified in the 'maxp' table<br />
     * Pushes	 -<br />
     * Related instructions	 ENDF[ ], CALL[ ]<br />
     * <p/>
     * Marks the start of a function definition and pops a number, f, from the
     * stack to uniquely identify this function. That definition will terminate
     * when an ENDF[] is encountered in the instruction stream. A function
     * definition can appear only in the font program or the CVT program.
     * Functions must be defined before they can be used with a CALL[ ] instruction.
     */
    public static final int FDEF = 0x2c;

    /**
     * FLIPOFF[] set the auto FLIP Boolean to OFF
     * <p/>
     * Code Range	 0x4E <br />
     * Pops	 -   <br />
     * Pushes	 -    <br />
     * Sets	 auto flip <br />
     * Affects	 MIRP, MIAP <br />
     * Related instructions	 FLIPON[ ], MIRP[ ], MIAP[ ]  <br />
     * <p/>
     * Sets the auto flip Boolean in the graphics state to FALSE causing the
     * MIRP[] and MIAP[] instructions to use the sign of control value table
     * entries. When auto flip is set to FALSE, the direction in which distances
     * are measured becomes significant. The default value for the auto flip
     * state variable is TRUE.
     */
    public static final int FLIPOFF = 0x4E;

    /**
     * FLIPON[] set the auto FLIP Boolean to ON
     * <p/>
     * Code Range	 0x4D <br />
     * Pops	 -     <br />
     * Pushes	 -  <br />
     * Sets	 auto flip  <br />
     * Affects	 MIRP, MIAP   <br />
     * Related instructions	 FLIPOFF[ ], MIRP[ ], MIAP[ ]  <br />
     * <p/>
     * Sets the auto flip Boolean in the graphics state to TRUE causing the
     * MIRP[] and MIAP[] instructions to ignore the sign of control value table
     * entries. When the auto flip variable is TRUE, the direction in which
     * distances are measured becomes insignificant. The default value for the
     * auto flip state variable is TRUE.
     */
    public static final int FLIPON = 0x4d;

    /**
     * FLIPPT[] FLIP PoinT
     * <p/>
     * Code Range	 0x80 <br />
     * Pops	 p1, p2, , ploopvalue: point number (uint32) <br />
     * Pushes	 - <br />
     * Uses	 zp0, loop <br />
     * Related instructions	 FLIPRGON[ ], FLIPRGOFF[ ]<br />
     * <p/>
     * Makes an on-curve point an off-curve point or an off-curve point an
     * on-curve point.
     * <p/>
     * Pops points, p, p1, p2, , ploopvalue from the stack. If pi is an on-curve
     * point it is made an off-curve point. If pi is an off-curve point it is
     * made an on-curve point. None of the points pi is marked as touched. As a
     * result, none of the flipped points will be affected by an IUP[ ] instruction.
     * A FLIPPT[ ] instruction redefines the shape of a glyph outline.
     */
    public static final int FLIPPT = 0x80;

    /**
     * FLIPRGOFF[] FLIP RanGe OFF
     * <p/>
     * Code Range	 0x82  <br />
     * Pops	 h: high point number in range (uint32) <br />
     * l: low point number in range (uint32)  <br />
     * Pushes	 -     <br />
     * Uses	 zp0    <br />
     * Related instructions	 FLIPPT[ ], FLIPRGOFF[ ]   <br />
     * <p/>
     * Changes all of the points in the range specified to off-curve points.
     * Pops two numbers defining a range of points, the first a highpoint and
     * the second a lowpoint. On-curve points in this range will become off-curve
     * points. The position of the points is not affected and accordingly the
     * points are not marked as touched.
     */
    public static final int FLIPRGOFF = 0x82;

    /**
     * FLIPRGON[] FLIP RanGe ON
     * <p/>
     * Code Range	 0x81 <br />
     * Pops	 h: highest point number in range of points to be flipped (uint32) <br />
     * l: lowest point number in range of points to be flipped (uint32)  <br />
     * Pushes	 -     <br />
     * Uses	 zp0   <br />
     * Related instructions	 FLIPPT[ ], FLIPRGOFF[ ] <br />
     * <p/>
     * Makes all the points in a specified range into on-curve points.
     * <p/>
     * Pops two numbers defining a range of points, the first a highpoint and
     * the second a lowpoint. Off-curve points in this range will become on-curve
     * points. The position of the points is not affected and accordingly the points
     * are not marked as touched.
     */
    public static final int FLIPRGON = 0x81;

    /**
     * FLOOR[] FLOOR
     * <p/>
     * Code Range	 0x66 <br />
     * Pops	 n: number whose floor is desired (F26Dot6)<br />
     * Pushes	 n : floor of n (F26Dot6)<br />
     * Related instructions	 CEILING[ ] <br />
     * <p/>
     * Takes the floor of the value at the top of the stack.
     * <p/>
     * Pops a 26.6 fixed point number n from the stack and returns n , the greatest
     * integer value less than or equal to n. Note that the floor of n, though an
     * integer value, is expressed as 26.6 fixed point number.
     */
    public static final int FLOOR = 0x66;

    /**
     * GC[a] Get Coordinate projected onto the projection vector
     * <p/>
     * Code Range	 0x46 - 0x47<br />
     * a	 0: use current position of point p  <br />
     * 1: use the position of point p in the original outline  <br />
     * Pops	 p: point number (uint32) <br />
     * Pushes	 c: coordinate location (F26Dot6) <br />
     * Uses	 zp2, projection vector, dual projection vector <br />
     * Related instructions	 SCFS[ ] <br />
     * <p/>
     * Gets the coordinate value of the specified point using the current projection vector.
     * <p/>
     * Pops a point number p and pushes the coordinate value of that point on the
     * current projection vector onto the stack. The value returned by GC[] is
     * dependent upon the current direction of the projection vector.
     * <p/>
     * The illustration below, GC[1], with p1 at the top of the stack, returns
     * the original position of point p1 while GC[0], with p2 at the top of the
     * stack, returns the current position of point p2.
     */
    public static final int GC_0 = 0x46;
    /**
     * GC[a] Get Coordinate projected onto the projection vector
     * <p/>
     * Code Range	 0x46 - 0x47<br />
     * a	 0: use current position of point p  <br />
     * 1: use the position of point p in the original outline  <br />
     * Pops	 p: point number (uint32) <br />
     * Pushes	 c: coordinate location (F26Dot6) <br />
     * Uses	 zp2, projection vector, dual projection vector <br />
     * Related instructions	 SCFS[ ] <br />
     * <p/>
     * Gets the coordinate value of the specified point using the current projection vector.
     * <p/>
     * Pops a point number p and pushes the coordinate value of that point on the
     * current projection vector onto the stack. The value returned by GC[] is
     * dependent upon the current direction of the projection vector.
     * <p/>
     * The illustration below, GC[1], with p1 at the top of the stack, returns
     * the original position of point p1 while GC[0], with p2 at the top of the
     * stack, returns the current position of point p2.
     */
    public static final int GC_1 = 0x47;

    /**
     * GETINFO[] GET INFOrmation
     * <p/>
     * Code Range	 0x88  <br />
     * Pops	 selector: integer (uint32)  <br />
     * Pushes	 result: integer (uint32) <br />
     * <p/>
     * Used to obtain data about the version of the TrueType engine that is
     * rendering the font as well as the characteristics of the current glyph.
     * The instruction pops a selector used to determine the type of information
     * desired and pushes a result onto the stack.
     * <p/>
     * Setting bit 0 in the selector requests the engine version. Setting bit 1
     * asks whether the glyph has been rotated. Setting bit 2 asks whether the
     * glyph has been stretched. To request information on two or more of these
     * values, set the appropriate bits. For example, a selector value of 6 (0112)
     * requests information on both rotation and stretching.
     * <p/>
     * The result is pushed onto the stack with the requested information. Bits 0
     * through 7 of result comprise the font engine version number. The version
     * numbers are listed in TABLE 0-2.
     * <p/>
     * Bit 8 is set to 1 if the current glyph has been rotated. It is 0 otherwise.
     * Bit 9 is set to 1 to indicate that the glyph has been stretched. It is 0 otherwise.
     * <p/>
     * TABLE 0-1 Selector bits and the results produced
     * <p/>
     * Table 6:
     * <table border='1'>
     * <tr><td>selector</td><td>bitsmeaning</td><td>result bits</tr>
     * <tr><td>0</td><td>get engine version</td><td>0-7</td></tr>
     * <tr><td>1</td><td>rotated?</td><td>8</td></tr>
     * <tr><td>2</td><td>stretched?</td><td>9</td></tr>
     * </table>
     * <p/>
     * The possible values for the engine version are given in TABLE 0-2.
     * <p/>
     * TABLE 0-2 Font engine version number
     * <table border='0'>
     * <tr><td>System</td><td>Engine Version </td></tr>
     * <tr><td>Macintosh System 6.0</td><td>1</td></tr>
     * <tr><td>Macintosh System 7.0</td><td>2 </td></tr>
     * <tr><td>Windows 3.1</td><td>3  </td></tr>
     * <tr><td>KanjiTalk 6.1</td><td> 4 </td></tr>
     * </table>
     * <p/>
     * If the TrueType engine is the System 7.0 version and the selector
     * requested information on the version number, rotation and stretching and
     * the glyph is rotated but not stretched, the result will be 01 0000 00102 or 258.
     */
    public static final int GETINFO = 0x88;

    /**
     * GFV[] Get Freedom Vector
     * <p/>
     * Code Range	 0x0D <br />
     * Pops	 -          <br />
     * Pushes	 px: x component (EF2Dot14)   <br />
     * py: y component (EF2Dot14)  <br />
     * Gets	 freedom vector    <br />
     * Related instructions	 GPV[ ]   <br />
     * <p/>
     * Decomposes the current freedom vector into its x and y components and puts
     * those components on the stack as two 2.14 numbers. The numbers occupy the
     * least significant two bytes of each long.
     * <p/>
     * The first component pushed, px, is the x-component of the freedom vector.
     * The second pushed, py, is the y-component of the freedom vector. Each is
     * a 2.14 number.
     * <p/>
     * GFV[] treats the freedom vector as a unit vector originating at the grid
     * origin. In the illustration below, the distance from point A to point B
     * is 1 unit.
     */
    public static final int GFV = 0x0d;

    /**
     * GPV[] Get Projection Vector
     * <p/>
     * Code Range	 0x0C <br />
     * Pops	 -   <br />
     * Pushes	 px: x component (EF2Dot14)   <br />
     * py: y component (EF2Dot14)<br />
     * Gets	 projection vector    <br />
     * Related instructions	 GFV[ ]<br />
     * <p/>
     * Decomposes the current projection vector into its x and y components and
     * pushes those components onto the stack as two 2.14 numbers.
     * <p/>
     * The first component pushed, px, is the x-component of the projection
     * vector. The second pushed, py, is the y-component of the projection vector.
     * <p/>
     * GPV[] treats the projection vector as a unit vector originating at the
     * grid origin. In the illustration below, the distance from point A to
     * point B is one unit.
     */
    public static final int GPV = 0x0c;

    /**
     * GT[] Greater Than
     * <p/>
     * Code Range	 0x52<br />
     * Pops	 e2: stack element e1: stack element <br />
     * Pushes	 b: Boolean value (uint32 in the range [0,1])<br />
     * Related instructions	 LT[ ], GTEQ[ ]<br />
     * <p/>
     * Compares the size of the top two stack elements.
     * <p/>
     * Pops two integers, e2 and e1, from the stack and compares them. If e1 is
     * greater than e2, one, signifying TRUE, is pushed onto the stack. If e1 is
     * not greater than e1, zero, signifying FALSE, is placed onto the stack.
     */
    public static final int GT = 0x52;

    /**
     * GTEQ[] Greater Than or EQual
     * <p/>
     * Code Range	 0x53  <br />
     * Pops	 e2: stack element e1: stack element  <br />
     * Pushes	 b: Boolean value (uint32 in the range [0,1]) <br />
     * Related instructions	 LTEQ[ ], GT[ ] <br />
     * <p/>
     * Compares the size of the top two stack elements.
     * <p/>
     * Pops two integers, e2 and e1, from the stack and compares them. If e1 is
     * greater than or equal to e2, one, signifying TRUE, is pushed onto the stack.
     * If e1 is not greater than or equal to e1, zero, signifying FALSE, is placed
     * onto the stack.
     */
    public static final int GTEQ = 0x53;

    /**
     * IDEF[] Instruction DEFinition
     * <p/>
     * Code Range	 0x89 <br />
     * Pops	 opcode (Eint8)<br />
     * Pushes	 -             <br />
     * Related instructions	 ENDF[ ]    <br />
     * <p/>
     * Begins the definition of an instruction. The instruction is identified by
     * the opcode popped. The intent of the IDEF[ ] instruction is to allow old
     * versions of the scaler to work with fonts that use instructions defined in
     * later releases of the TrueType interpreter. Referencing an undefined opcode
     * will have no effect. The IDEF[ ] is not intended for creating user defined
     * instructions. The FDEF[ ] should be used for that purpose.
     * <p/>
     * The instruction definition that began with the IDEF[ ] terminates when an
     * ENDF[ ] is encountered in the instruction stream. Nested IDEFs are not
     * allowed. Subsequent executions of the opcode popped will be directed to
     * the contents of this instruction definition. IDEFs should be defined in
     * the font program. Defining instructions in the CVT program is not
     * recommended.
     */
    public static final int IDEF = 0x89;

    /**
     * IF[] IF test
     * <p/>
     * Code Range	 0x58  <br />
     * Pops	 e: stack element <br />
     * Pushes	 -  <br />
     * Related instructions	 ELSE[ ], EIF[ ]    <br />
     * <p/>
     * Marks the beginning of an if-statement.
     * <p/>
     * Pops an integer, e, from the stack. If e is zero (FALSE), the instruction
     * pointer is moved to the associated ELSE or EIF[] instruction in the
     * instruction stream. If e is nonzero (TRUE), the next instruction in the
     * instruction stream is executed. Execution continues until the associated
     * ELSE[] instruction is encountered or the associated EIF[] instruction ends
     * the IF[] statement. If an associated ELSE[] statement is found before the
     * EIF[], the instruction pointer is moved to the EIF[] statement.
     */
    public static final int IF = 0x58;

    /**
     * INSTCTRL INSTRuction execution ConTRoL
     * <p/>
     * Code Range	 0x8E <br />
     * Pops	 s: selector (int32) v: value for instruction control (int32)  <br />
     * Pushes	 -   <br />
     * Sets	 instruction control<br />
     * <p/>
     * Sets the instruction control state variable making it possible to turn on
     * or off the execution of instructions and to regulate use of parameters
     * set in the CVT program.
     * <p/>
     * This instruction clears and sets various control flags. The selector is
     * used to choose the relevant flag. The value determines the new setting
     * of that flag.
     * <p/>
     * In the version 1.0 there are only two flags in use.
     * Flag 1 is used to inhibit grid-fitting. It is chosen with a selector value
     * of 1. If this flag is set to TRUE (v=1), any instructions associated with
     * glyphs will not be executed. If the flag is FALSE (v=0), instructions will
     * be executed. For example, to inhibit grid-fitting when a glyph is being
     * rotated or stretched, use the following sequence on the preprogram:
     * <p/>
     * PUSHB[000] 6	 // ask GETINFO to check for stretching or rotation <br />
     * GETINFO[ ]	 // will push TRUE if glyph is stretched or rotated <br />
     * IF[]	 // tests value at top of stack <br />
     * PUSHB[000] 1	 // value for INSTCTRL <br />
     * PUSHB[000] 1	 // selector for INSTCTRL  <br />
     * INSTCTRL[]	 // based on selector and value will turn grid-fitting off  <br />
     * EIF[]  <br />
     * <p/>
     * Flag 2 is used to establish that any parameters set in the CVT program
     * should be ignored when instructions associated with glyphs are executed.
     * These include, for example, the values for scantype and the CVT cut-in.
     * When flag2 is set to TRUE the default values of those parameters will be
     * used regardless of any changes that may have been made in those values by
     * the preprogram. When flag2 is set to FALSE, parameter values changed by
     * the CVT program will be used in glyph instructions.
     * <p/>
     * INSTCTRL[] can only be executed in the CVT program.
     */
    public static final int INSTCTRL = 0x8e;

    /**
     * IP[] Interpolate Point
     * <p/>
     * Code Range	 0x39<br />
     * Pops	 p1, p2, , ploopvalue: point number (uint32)<br />
     * Pushes	 -  <br />
     * Uses	 zp0 with rp1, zp1 with rp2, zp2 with point p, loop, freedom vector,
     * projection vector, dual projection vector <br />
     * <p/>
     * Related instructions	 IUP[ ]
     * <p/>
     * Interpolates the position of the specified points to preserve their original
     * relationship with the reference points rp1 and rp2.
     * <p/>
     * Pops point numbers, p1, p2, , ploopvalue, from the stack. Moves each point
     * pi so that its relationship to rp1 and rp2 is the same as it was in the
     * original uninstructed outline. That is, the following relationship holds:
     * <p/>
     * This instruction is illegal if rp1 and rp2 occupy the same position on the
     * projection vector.
     * <p/>
     * More intuitively, an IP[] instruction preserves the relative relationship
     * of a point relative to two reference points.
     * <p/>
     * In the illustrations below, point p is interpolated relative to reference
     * points rp1 and rp2. In the first illustration, which depicts the situation
     * before the IP[] instruction is executed, the distance from of point p to
     * the original position of rp1 is d1 and the distance from point p to the
     * original position of point rp2 is d2. The ratio of the two distances is
     * d1:d2.
     * <p/>
     * The effect of the IP[] instruction is shown in the illustration below.
     * It moves point p along the freedom vector until the ratio of the distance,
     * d3, from the current position of rp1 to point p, to the distance, d4, from
     * point p to the current position of point rp2 is equal to d1:d2. That is,
     * point p is moved along the freedom vector until d1:d2 = d3:d4.when these
     * distances are measured along the projection vector.
     */
    public static final int IP = 0x39;

    /**
     * ISECT[] moves point p to the InterSECTion of two lines
     * <p/>
     * Code Range	 0x0F<br />
     * Pops	 a0: start point of line A (uint32) <br />
     * a1: end point of line A (uint32)<br />
     * b0: start point of line B (uint32)<br />
     * b1: end point of line B (uint32)<br />
     * p: point to move (uint32) Pushes - <br />
     * <p/>
     * Uses	 zp2 with point p, zp0 with line A, zp1 with line B
     * <p/>
     * Moves the specified point to the intersection of the two lines specified.
     * <p/>
     * Pops the end points of line A, a0 and a1, followed by the end points of
     * line B, b0 and b1 followed by point p. Puts point p at the intersection of
     * the lines A and B. The points a0 and a1 define line A. Similarly, b0 and b1
     * define line B. ISECT ignores the freedom vector in moving point p.
     * <p/>
     * In the degenerate case of parallel lines A and B, the point is put in the
     * middle. That is.
     * <p/>
     * In the illustration below, point p is moved from its current position to
     * the intersection of the line defined by a0, a1 and the line defined by b0,
     * b1. Note that point p need not move along the freedom vector but is simply
     * relocated at the point of intersection.
     */
    public static final int ISECT = 0x0f;

    /**
     * IUP[a] Interpolate Untouched Points through the outline
     * <p/>
     * Code Range	 0x30 - 0x31 <br />
     * a	 0: interpolate in the y-direction<br />
     * 1: interpolate in the x-direction <br />
     * Pops	 - <br />
     * Pushes	 - <br />
     * Uses	 zp2  <br />
     * Related instructions	 IP[ ] <br />
     * <p/>
     * Interpolates untouched points in the zone referenced by zp2 to preserve
     * the original relationship of the untouched points to the other points in
     * that zone.
     * <p/>
     * Considers the reference glyph outline contour by contour, moving any
     * untouched points that fall sequentially between a pair of touched points.
     * How such a point is moved, however, depends on whether its projection fall
     * between the projections of the touched points. That is, if the projected
     * x-coordinate or y-coordinate (depending on whether the interpolation is in
     * x or in y) of an untouched point were originally between those of the touched
     * pair, that coordiante is linearly interpolated between the new coordinates
     * of the touched points. Otherwise the untouched point is shifted by the amount
     * the nearest touched point was shifted from its original outline position.
     * The value of the Boolean a, determines whether the interpolation will be
     * in the x-direction or the y-direction. The current settings of the freedom
     * and projection vectors are not relevant.
     * <p/>
     * The set of fiigures below illustrates this distinction. The first
     * illustration shows the contour before the IUP[] instruction is executed.
     * Here p1, p2, p3, p4 and p5 are consecutive points on a contour. Point p2,
     * p3 and p4 all fall sequentially between p1 and p5 on the contour. Assume
     * that point p3 has been touched.
     * <p/>
     * Point p4 has an x-coordinate that is between p1 and p5 while points p2 and
     * p3 do not. Assume that p1 and p5 have been moved by a previous instructions
     * and that point p3 has been touched but not moved from its original position.
     * As a result of an IUP[1] an interpolation in the x--direction takes place.
     * Point p4 will be linearly interpolated. Point p2 will be shifted by the amount
     * the nearest touched point was shifted. Point p3 will be unaffected. (Points p2
     * and p4 are assumed to be in their original position. This is not strictly
     * necessary as a point that has been moved can be untouched with the UTP[ ]
     * instruction and hence subject to the actions of an IUP[ ] instruction.)
     * <p/>
     * As the result of the IUP[1] instruction, two points are moved. The first
     * move is the shift illustrated below. Point p1 has moved a distance ds units
     * parallel to the x-axis from its original position. Point p2 is moved parallel
     * to the x-axis until it is at a distance equal to ds from its original position.
     * <p/>
     * The second move is the linear interpolation shown in the illustration below.
     * Point p4 is moved along the specified axis to a new position that preserves
     * its relative distance from points p1 and p5. After the interpolation the
     * ratio of the original distance from point p4 to p1 (d1) to the original
     * distance of point p4 to p5 (d2) is the same as the ratio of the new
     * distance from point p4 to p1(d3) to the new distance of point p4 to p4 (d4).
     * That is: d1:d2 = d3:d4
     * <p/>
     * This instruction operates on points in the glyph zone pointed to by zp2.
     * This zone should always be zone 1. Applying IUP[ ] to zone 0 is illegal.
     * <p/>
     * The IUP[ ] instruction does not touch the points it moves. Thus the untouched
     * points affected by an IUP[ ] instruction will be affected by subsequent IUP[]
     * instructions unless they are touched by an intervening instruction.
     */
    public static final int IUP_0 = 0x30;
    /**
     * see ISECT_0
     */
    public static final int IUP_1 = 0x31;

    /**
     * JMPR JuMP Relative
     * <p/>
     * Code Range	 0x1C <br />
     * Pops	 offset: number of bytes to move instruction pointer (int32) <br />
     * Pushes	 -   <br />
     * Related instructions	 JROF[ ], JROT[ ]   <br />
     * <p/>
     * Moves the instruction pointer to a new location specified by the offset
     * popped from the stack.
     * <p/>
     * Pops an integer offset from the stack. The signed offset is added to the
     * instruction pointer and execution is resumed at the new location in the
     * instruction steam. The jump is relative to the position of the instruction
     * itself. That is, an offset of +1 causes the instruction immediately
     * following the JMPR[] instruction to be executed.
     */
    public static final int JMPR = 0x1c;

    /**
     * JROF[] Jump Relative On False
     * <p/>
     * Code Range	 0x79<br />
     * Pops	 e: stack element offset: number of bytes to move instruction pointer (int32)  <br />
     * Pushes	 - <br />
     * Related instructions	 JMPR[ ] JROT[ ] <br />
     * <p/>
     * Moves the instruction pointer to a new location specified by the offset
     * popped from the stack if the element tested has a FALSE (zero) value.
     * <p/>
     * Pops a Boolean value, e and an offset. In the case where the Boolean, e,
     * is FALSE, the signed offset will be added to the instruction pointer and
     * execution will be resumed at the new location; otherwise, the jump is not
     * taken. The jump is relative to the position of the instruction itself.
     */
    public static final int JROF = 0x79;

    /**
     * JROT[] Jump Relative On True
     * <p/>
     * Code Range	 0x78  <br />
     * Pops	 e: stack element   <br />
     * offset: number of bytes to move    <br />
     * instruction pointer (int32) <br />
     * Pushes	 -  <br />
     * Related instructions	 JMPR[ ] JROF[ ] <br />
     * <p/>
     * Moves the instruction pointer to a new location specified by the offset
     * value popped from the stack if the element tested has a TRUE value.
     * <p/>
     * Pops a Boolean value, e and an offset. If the Boolean is TRUE (non-zero)
     * the signed offset will be added to the instruction pointer and execution
     * will be resumed at the address obtained. Otherwise, the jump is not taken.
     * The jump is relative to the position of the instruction itself.
     */
    public static final int JROT = 0x78;

    /**
     * LOOPCALL[] LOOP and CALL function
     * <p/>
     * Code Range	 0x2A  <br />
     * Pops	 f: function number integer in the range 0 through (n-1) where n is specified in the 'maxp' table <br />
     * count: number of times to call the function (signed word)<br />
     * Pushes	 - <br />
     * Related instructions	 SLOOP[ ] <br />
     * <p/>
     * Repeatedly calls a function.
     * <p/>
     * Pops a function number f and a count. Calls the function, f, count number
     * of times.
     */
    public static final int LOOPCALL = 0x2a;

    /**
     * LT[] Less Than
     * <p/>
     * Code Range	 0x50 <br />
     * Pops	 e2: stack element (StkElt) <br />
     * e1: stack element (StkElt)<br />
     * Pushes	 b: Boolean value (uint32 in the range [0,1]) <br />
     * Related instructions	 GT[ ], LTEQ[ ]<br />
     * <p/>
     * Compares the two number at the top of the stack. The test succeeds if the
     * second of the two numbers is smaller than the first.
     * <p/>
     * Pops two integers from the stack, e2 and e1, and compares them. If e1 is
     * less than e2, 1, signifying TRUE, is pushed onto the stack. If e1 is not
     * less than e2, 0, signifying FALSE, is placed onto the stack.
     */
    public static final int LT = 0x50;

    /**
     * LTEQ[] Less Than or Equal
     * <p/>
     * Code Range	 0x51<br />
     * Pops	 e2: stack element <br />
     * e1: stack element <br />
     * Pushes	 b: Boolean value (uint32 in the range [0,1])<br />
     * Related instructions	 GTEQ[ ], LT[ ] <br />
     * <p/>
     * Compares the two numbers at the top of the stack. The test succeeds if
     * the second of the two numbers is smaller than or equal to the first.
     * <p/>
     * Pops two integers, e2 and e1 from the stack and compares them. If e1 is
     * less than or equal to e2, one, signifying TRUE, is pushed onto the stack.
     * If e1 is greater than e2, zero, signifying FALSE, is placed onto the stack.
     */
    public static final int LTEQ = 0x51;

    /**
     * MAX[] MAXimum of top two stack elements
     * <p/>
     * Code Range	 0x8B <br />
     * Pops	 e2: stack element  <br />
     * e1: stack element  <br />
     * Pushes	 maximum of e1 and e2<br />
     * Related instructions	 MIN[ ] <br />
     * <p/>
     * Returns the larger of the top two stack elements.
     * <p/>
     * Pops two elements, e2 and e1, from the stack and pushes the larger of
     * these two quantities onto the stack.
     */
    public static final int MAX = 0x8b;

    /**
     * MD[a] Measure Distance
     * <p/>
     * Code Range	 0x49 - 0x4A<br />
     * a	 0: measure distance in grid-fitted outline    <br />
     * 1: measure distance in original outline    <br />
     * Pops	 p2: point number (uint32) p1: point number (uint32)    <br />
     * Pushes	 d: distance (F26Dot6) <br />
     * <p/>
     * Uses zp0 with point p1, zp1 with point p2, projection vector, dual
     * projection vector
     * <p/>
     * Measures the distance between the two points specified.
     * <p/>
     * Pops two point numbers p2 and p1 and measures the distance between the
     * two points specified. The distance, d, is pushed onto the stack as a pixel
     * coordinate. The distance is signed. Reversing the order in which the points
     * are listed will change the sign of the result.
     * <p/>
     * Depending upon the setting of the Boolean variable a, distance will be
     * measured in the original outline or the grid-fitted outline. MD[0] measures
     * the distance in the original outline while MD[1] measures the distance in
     * the grid-fitted outline. As always, distance is measured along the projection
     * vector. Just as reversing the order in which the points are listed will
     * change the sign of the distance, reversing the orientation of the projection
     * vector will have the same effect.
     * <p/>
     * In the example below, MD[1] will yield the original outline distance from
     * point p1 to point p2. MD[0] will yield the distance from point p1 to point p2.
     */
    public static final int MD_0 = 0x49;

    /**
     * MD[a] Measure Distance
     * see MAD_0
     */
    public static final int MD_1 = 0x4A;

    /**
     * MDAP[a] Move Direct Absolute Point
     * <p/>
     * Code Range	 0x2E - 0x2F   <br />
     * a:	 0: do not round the value   <br />
     * 1: round the value       <br />
     * Pops	 p: point number (uint32)  <br />
     * Pushes	 -    <br />
     * Sets	 rp0 and rp1 are set to point p  <br />
     * Uses	 zp0, freedom vector, projection vector, round state    <br />
     * Related instructions	 MDRP[ ], MIAP[ ]  <br />
     * <p/>
     * Touch and, in some cases, round the specified point. A point that is
     * "dapped" will be unaffected by subsequent IUP[ ] instructions and is
     * generally intended to serve as a reference point for future instructions.
     * Dapping a point with rounding set to grid will cause the point to have an
     * integer valued coordinate along the projection vector. If the projection
     * vector is set to the x-axis or y-axis, this will cause the point to be
     * <p/>
     * grid-aligned.
     * Pops a point number, p, and sets reference points rp0 and rp1 to point p.
     * If the Boolean a is set to 1, the coordinate of point p, as measured against
     * the projection vector, will be rounded and then moved the rounded distance
     * from its current position. If the Boolean a is set to 0, point p is not
     * moved, but nonetheless is marked as touched in the direction(s) specified
     * by the current freedom vector.
     */
    public static final int MDAP_0 = 0x2e;

    /**
     * MDAP[a] Move Direct Absolute Point, round the value
     * SEE MDAP_1
     */
    public static final int MDAP_1 = 0x2f;

    /**
     * MDRP[abcde] Move Direct Relative Point
     * <p/>
     * Code Range	 0xC0 - 0xDF <br />
     * a	 0: do not reset rp0 to point p<br />
     * 1: set rp0 to point p <br />
     * b	 0: do not keep distance greater than or equal to minimum distance <br />
     * 1: keep distance greater than or equal to minimum distance<br />
     * c	 0: do not round distance <br />
     * 1: round the distance <br />
     * de	 distance type for engine characteristic compensation<br />
     * Pops	 p: point number (uint32)<br />
     * Pushes	 - <br />
     * Sets	 after point p is moved, rp1 is set equal to rp0, rp2 is set equal to point number p; if the a flag is set to TRUE, rp0 is set equal to point number p<br />
     * Uses	 zp0 with rp0 and zp1 with point p, minimum distance, round state,single width value, single width cut-in, freedom vector, projection vector, dual projection vector<br />
     * Related instructions	 MDAP[ ], MIRP[ ]<br />
     * <p/>
     * Preserves the master outline distance between the specified point and the
     * reference point rp0.
     * <p/>
     * Pops a point number, p, and moves point p along the freedom vector so that
     * the distance from its new position to the current position of rp0 is the
     * same as the distance between the two points in the original uninstructed
     * outline, and then adjusts it to be consistent with the Boolean settings.
     * Note that it is only the original positions of rp0 and point p and the
     * current position of rp0 that determine the new position of point p along the freedom vector.
     * <p/>
     * MDRP[] is typically used to control the width or height of a glyph feature
     * using a value which comes from the original outline. Since MDRP[] uses a
     * direct measurement and does not reference the control value cut-in, it is
     * used to control measurements that are unique to the glyph being instructed.
     * Where there is a need to coordinate the control of a point with the treatment
     * of points in other glyphs in the font, a MIRP[] instruction is needed.
     * <p/>
     * Though MDRP[] does not refer to the CVT, its effect does depend upon the
     * single-width cut-in value. If the device space distance between the measured
     * value taken from the uninstructed outline and the single width value is less
     * than the single width cut-in, the single width value will be used in preference
     * to the outline distance. In other words, if the two distances are sufficiently
     * close (differ by less than the single width cut-in), the single width value
     * will be used.
     * <p/>
     * The setting of the round state graphics state variable will determine whether
     * and how the distance of point p from rp0 is rounded. If the round bit is not
     * set, the value will be unrounded. If the round bit is set, the effect will
     * depend upon the choice of rounding state.
     * <p/>
     * A MDRP[] instruction can also be set to use the minimum distance value.
     * Minimum distance sets a lower bound on the value the distance between two
     * points can be rounded to.
     * <p/>
     * Distances measured with the MDRP[] instruction, like all TrueType distances,
     * must be either black, white or grey. Indicating this value in Booleans de
     * allows the interpreter to compensate for engine characteristics as needed.
     * <p/>
     * The illustration below, point p is moved along the freedom vector from its
     * current position to a new position that is a distance, d from the reference
     * point rp0. This distance is the same as the original distance from p to rp0.
     */
    public static final int MDRP_0 = 0xc0;
    public static final int MDRP_1 = 0xc1;
    public static final int MDRP_2 = 0xc2;
    public static final int MDRP_3 = 0xc3;
    public static final int MDRP_4 = 0xc4;
    public static final int MDRP_5 = 0xc5;
    public static final int MDRP_6 = 0xc6;
    public static final int MDRP_7 = 0xc7;
    public static final int MDRP_8 = 0xc8;
    public static final int MDRP_9 = 0xc9;
    public static final int MDRP_10 = 0xca;
    public static final int MDRP_11 = 0xcb;
    public static final int MDRP_12 = 0xcc;
    public static final int MDRP_13 = 0xcd;
    public static final int MDRP_14 = 0xce;
    public static final int MDRP_15 = 0xcf;
    public static final int MDRP_16 = 0xd0;
    public static final int MDRP_17 = 0xd1;
    public static final int MDRP_18 = 0xd2;
    public static final int MDRP_19 = 0xd3;
    public static final int MDRP_20 = 0xd4;
    public static final int MDRP_21 = 0xd5;
    public static final int MDRP_22 = 0xd6;
    public static final int MDRP_23 = 0xd7;
    public static final int MDRP_24 = 0xd8;
    public static final int MDRP_25 = 0xd9;
    public static final int MDRP_26 = 0xda;
    public static final int MDRP_27 = 0xdb;
    public static final int MDRP_28 = 0xdc;
    public static final int MDRP_29 = 0xdd;
    public static final int MDRP_30 = 0xde;
    public static final int MDRP_31 = 0xdf;


    /**
     * MIAP[a] Move Indirect Absolute Point
     * <p/>
     * Code Range	 0x3E - 0x3F <br />
     * a	 0: don't round the distance and don't look at the control value cut-in   <br />
     * 1: round the distance and look at the control value cut-in  <br />
     * Pops	 n: CVT entry number (F26Dot6)    <br />
     * p: point number (uint32)  <br />
     * Pushes	 -                <br />
     * Sets	 set rp0 and rp1 to point p    <br />
     * Uses	 zp0, round state, control value cut-in, freedom vector, projection vector  <br />
     * Related instructions	 MSIRP[ ], MIRP[ ], MDAP[ ] <br />
     * <p/>
     * Makes it possible to coordinate the location of a point with that of
     * other similar points by moving that point to a location specified in the
     * control value table.
     * <p/>
     * Pops a CVT entry number n and a point number p and then moves point p to
     * the absolute coordinate position specified by the nth control value table
     * entry. The coordinate is measured along the current projection vector. If
     * boolean a has the value one, the position will be rounded as specified by
     * round state. If boolean a has the value one and the device space difference
     * between the CVT value and the original position is greater than the control
     * value cut-in, the original position will be rounded (instead of the CVT value.)
     * <p/>
     * The a Boolean above controls both rounding and the use of the control
     * value cut-in. To have this Boolean specify only whether or not the MIAP[]
     * instruction should look at the control value cut-in value, use the ROFF[]
     * instruction to turn off rounding.
     * <p/>
     * This instruction can be used to "create" twilight zone points. This is
     * accomplished by setting zp0 to zone 0 and moving the specified point,
     * which is initially at the origin to the desired location.
     * <p/>
     * In the illustration below, point p is moved along the freedom vector until
     * it occupies a position that projects to c units along the projection vector.
     */
    public static final int MIAP_0 = 0x3e;

    /**
     * MIAP[a] Move Indirect Absolute Point
     * <p/>
     * 1 - round the distance and look at the control value cut-in<br />
     * see MIAP_0
     */
    public static final int MIAP_1 = 0x3f;

    /**
     * MIN[] MINimum of top two stack elements
     * <p/>
     * Code Range	 0x8C <br />
     * Pops	 e2: stack element e1: stack element<br />
     * Pushes	 minimum of e1 and e2<br />
     * Related instructions	 MAX[ ]<br />
     * Returns the minimum of the top two stack elements.<br />
     * <p/>
     * Pops two elements, e2 and e1, from the stack and pushes the smaller of
     * these two quantities onto the stack.
     */
    public static final int MIN = 0x8c;

    /**
     * MINDEX[] Move the INDEXed element to the top of the stack
     * <p/>
     * Code Range	 0x26 <br />
     * Pops	 k: stack element <br />
     * Pushes	 ek: stack element <br />
     * Stack before	 k: stack element number (uint32)   <br />
     * e1: stack element <br />
     * ... <br />
     * ek-1: stack element <br />
     * ek: stack element<br />
     * Stack after	 ek: indexed element <br />
     * e1: stack element<br />
     * ...  <br />
     * ek-1: stack element   <br />
     * Related instructions	 CINDEX[ ]  <br />
     * <p/>
     * Moves the indexed element to the top of the stack thereby removing it from its original position.
     * <p/>
     * Pops an integer, k, from the stack and moves the element with index k to the top of the stack.
     */
    public static final int MINDEX = 0x26;

    /**
     * MIRP[abcde] Move Indirect Relative Point
     * <p/>
     * Code Range	 0xE0 - 0xFF<br />
     * a	 0: Do not set rp0 to p <br />
     * 1: Set rp0 to p  <br />
     * b	 0: Do not keep distance greater than or equal to minimum distance <br />
     * 1: Keep distance greater than or equal to minimum distance   <br />
     * c	 0: Do not round the distance and do not look at the control value cut-in  <br />
     * 1: Round the distance and look at the control value cut-in value <br />
     * de:	 distance type for engine characteristic compensation  <br />
     * Pops	 n: CVT entry number (F26Dot6) p: point number (uint32) <br />
     * Pushes	 -  <br />
     * Uses	 zp0 with rp0 and zp1 with point p. round state, control value cut-in, single width value, single width cut-in, freedom vector, projection vector, auto flip, dual projection vector<br />
     * Sets	 After it has moved the point this instruction sets rp1 equal to rp0, rp2 is set equal to point number p; lastly, if a has the value TRUE, rp0 is set to point number p.<br />
     * Related instructions	 MSIRP[ ], MIAP[ ], MDRP[ ]  <br />
     * <p/>
     * Makes it possible to coordinate the distance between a point and a
     * reference point with other similar distances by making that distance
     * subject to a control value table entry.
     * <p/>
     * Moves point p along the freedom vector so that the distance from p to
     * the current position of rp0 is equal to the distance stated in the referenced
     * CVT entry, assuming that the cut-in test succeeds. Note that in making the
     * cut-in test, MIRP[] uses the original outline distance between p and rp0.
     * If the cut-in test fails, point p will be moved so that its distance from
     * the current position of rp0 is equal to the original outline distance
     * between p and the point referenced by rp0.
     * <p/>
     * A MIRP[] instruction makes it possible to preserve the distance between two
     * points subject to a number of qualifications. Depending upon the setting
     * of Boolean flag b, the distance can be kept greater than or equal to the
     * value established by the minimum distance state variable. Similarly, the
     * instruction can be set to round the distance according to the round state
     * graphics state variable. The value of the minimum distance variable is the
     * smallest possible value the distance between two points can be rounded to.
     * Additionally, if the c Boolean is set, the MIRP[] instruction acts subject
     * to the control value cut-in. If the difference between the actual measurement
     * and the value in the CVT is sufficiently small (less than the cut-in value),
     * the CVT value will be used and not the actual value. If the device space
     * difference between the CVT value and the single width value is smaller than
     * the single width cut-in, then use the single width value rather than the
     * control value table distance.
     * <p/>
     * The c Boolean above controls both rounding and the use of control value
     * table entries. If you would like the meaning of this Boolean to specify
     * only whether or not the MIRP[] instruction should look at the control
     * value cut-in, use the ROFF[] instruction to turn off rounding. In this
     * manner, it is possible to specify that rounding is off but the cut-in still applies.
     * <p/>
     * MIRP[] can be used to create points in the twilight zone.
     * <p/>
     * In the illustration below, point p is moved along the freedom vector until
     * its distance to point rp0 is equal to the distance d found in the reference
     * CVT entry.
     */
    public static final int MIRP = 0xE0;
    public static final int MIRP_1 = 0xE1;
    public static final int MIRP_2 = 0xE2;
    public static final int MIRP_3 = 0xE3;
    public static final int MIRP_4 = 0xE4;
    public static final int MIRP_5 = 0xE5;
    public static final int MIRP_6 = 0xE6;
    public static final int MIRP_7 = 0xE7;
    public static final int MIRP_8 = 0xE8;
    public static final int MIRP_9 = 0xE9;
    public static final int MIRP_10 = 0xEA;
    public static final int MIRP_11 = 0xEB;
    public static final int MIRP_12 = 0xEC;
    public static final int MIRP_13 = 0xED;
    public static final int MIRP_14 = 0xEE;
    public static final int MIRP_15 = 0xEF;
    public static final int MIRP_16 = 0xF0;
    public static final int MIRP_17 = 0xF1;
    public static final int MIRP_18 = 0xF2;
    public static final int MIRP_19 = 0xF3;
    public static final int MIRP_20 = 0xF4;
    public static final int MIRP_21 = 0xF5;
    public static final int MIRP_22 = 0xF6;
    public static final int MIRP_23 = 0xF7;
    public static final int MIRP_24 = 0xF8;
    public static final int MIRP_25 = 0xF9;
    public static final int MIRP_26 = 0xFA;
    public static final int MIRP_27 = 0xFB;
    public static final int MIRP_28 = 0xFC;
    public static final int MIRP_29 = 0xFD;
    public static final int MIRP_30 = 0xFE;
    public static final int MIRP_31 = 0xFF;

    /**
     * MPPEM[] Measure Pixels Per EM
     * <p/>
     * Code Range	 0x4B <br />
     * Pops	 - <br />
     * Pushes	 ppem: pixels per em (Euint16) <br />
     * Uses	 projection vector      <br />
     * Related instructions	 MPS[ ]  <br />
     * <p/>
     * Pushes the current number of pixels per em onto the stack. Pixels per em
     * is a function of the resolution of the rendering device and the current
     * point size and the current transformation matrix. This instruction looks
     * at the projection vector and returns the number of pixels per em in that
     * direction. The number is always an integer.
     * <p/>
     * The illustration below shows magnifications of an 18 point Times New Roman
     * s at 72 dpi, 144 dpi, and 300 dpi, respectively. Increasing the number of
     * pixels per em improves the quality of the image obtained. It does not,
     * however, change the absolute size of the image obtained.
     */
    public static final int MPPEM = 0x4B;

    /**
     * MPS[] Measure Point Size
     * <p/>
     * Code Range	 0x4C  <br />
     * Pops	 -   <br />
     * Pushes	 pointSize: the current point size(Euint16)   <br />
     * Related instructions	 MPPEM[ ]    <br />
     * Pushes the current point size onto the stack.    <br />
     * <p/>
     * Measure point size can be used to obtain a value which serves as the basis
     * for choosing whether to branch to an alternative path through the instruction
     * stream. It makes it possible to treat point sizes below or above a certain
     * threshold differently.
     * <p/>
     * The illustration below shows magnifications of 12 point, 24 point, and
     * 48point Times New Roman Q at 72 dpi. Note that increasing the point size
     * of a glyph increases its absolute size. On a low resolution device, like a
     * screen, more detail can be captured at a higher point size.
     */
    public static final int MPS = 0x4c;

    /**
     * MSIRP[a] Move Stack Indirect Relative Point
     * <p/>
     * Code Range	 0x3A - 0x3B  <br />
     * * a	 0: do not change rp0  <br />
     * 1: set rp0 to point number p  <br />
     * Pops	 d: distance (F26Dot6) p: point number (uint32)   <br />
     * Pushes	 -    <br />
     * Uses	 zp1 with point p and zp0 with rp0, freedom vector, projection vector  <br />
     * Related instructions	 MIRP[ ] <br />
     * <p/>
     * Makes it possible to coordinate the distance between a point and a reference
     * point by setting the distance from a value popped from the stack.
     * <p/>
     * Pops a distance, d and a point number, p, and makes the distance between point
     * p and the current position of rp0 equal to d. The distance, d, is in pixel
     * coordinates.
     * <p/>
     * MSIRP[ ] is very similar to the MIRP[ ] instruction except for taking the
     * distance from the stack rather than the CVT. Since MSIRP[ ] does not use
     * the CVT, the control value cut-in is not a factor as it is in MIRP[ ].
     * Since MSIRP[ ] does not round, its effect is not dependent upon the round state.
     * <p/>
     * MSIRP[] can be used to create points in the twilight zone.
     * <p/>
     * In the illustration below, point p is moved along the freedom vector until
     * it is at a distance d from rp0.
     */
    public static final int MSIRP_0 = 0x3a;
    public static final int MSIRP_1 = 0x3b;

    /**
     * MUL[] MULtiply
     * <p/>
     * Code Range	 0x63  <br />
     * Pops	 n2: multiplier (F26Dot6)    <br />
     * n1: multiplicand (F26Dot6) <br />
     * Pushes	 (n2 * n1)/64: product (F26Dot6)  <br />
     * Related instructions	 DIV[ ] <br />
     * <p/>
     * Multiplies the top two numbers on the stack. Pops two 26.6 numbers, n2 and n1,
     * from the stack and pushes onto the stack the product of the two elements.
     * The 52.12 result is shifted right by 6 bits and the high 26 bits are
     * discarded yielding a 26.6 result.
     */
    public static final int MUL = 0x63;

    /**
     * NEG[] NEGate
     * <p/>
     * Code Range	 0x65<br />
     * Pops	 n: pixel coordinate (F26Dot6)<br />
     * Pushes	 -n: negation of n1 (F26Dot6)<br />
     * <p/>
     * Negates the number at the top of the stack.
     * Pops a number, n, from the stack and pushes the negated value of n onto
     * the stack.
     */
    public static final int NEG = 0x65;

    /**
     * NEQ[] Not EQual
     * <p/>
     * Code Range	 0x55  <br />
     * Pops	 e2: stack element  <br />
     * e1: stack element  <br />
     * Pushes	 b: Boolean value (uint32 in the range [0,1])   <br />
     * Related instructions	 EQ[ ] <br />
     * <p/>
     * Determines whether the two elements at the top of the stack are unequal.
     * <p/>
     * Pops two numbers, e2 and e1, from the stack and compares them. If they
     * are different, one, signifying TRUE is pushed onto the stack. If they are
     * equal, zero, signifying FALSE is pushed onto the stack.
     * <p/>
     * Return to Contents
     */
    public static final int NEQ = 0x55;

    /**
     * NOT[] logical NOT
     * <p/>
     * Code Range	 0x5C <br />
     * Pops	 e: stack element <br />
     * Pushes	 (not e): logical negation of e (uint32)  <br />
     * <p/>
     * Takes the logical negation of the number at the top of the stack.
     * <p/>
     * Pops a number e from the stack and returns the result of a logical NOT
     * operation performed on e. If e was zero, one is pushed onto the stack if
     * e was nonzero, zero is pushed onto the stack.
     */
    public static final int NOT = 0x5C;

    /**
     * NPUSHB[] PUSH N Bytes
     * <p/>
     * Code Range	 0x40   <br />
     * From IS	 n: number of bytes to push (1 byte interpreted as an integer)<br />
     * b1, b2,...bn: sequence of n bytes<br />
     * Pushes	 b1, b2,...bn: sequence of n bytes each extended to 32 bits (uint32)<br />
     * Related instructions	 NPUSHW[ ], PUSHB[ ], PUSHW[] <br />
     * <p/>
     * Takes n bytes from the instruction stream and pushes them onto the stack.
     * <p/>
     * Looks at the next byte in the instructions stream, n, and takes n unsigned
     * bytes from the instruction stream, where n is an unsigned integer in the
     * range (0 255), and pushes them onto the stack. The number of bytes to push,
     * n, is not pushed onto the stack.
     * <p/>
     * Each byte value is unsigned extended to 32 bits before being pushed onto
     * the stack.
     */
    public static final int NPUSHB = 0x40;

    /**
     * NPUSHW[] PUSH N Words
     * <p/>
     * Code Range	 0x41<br />
     * From IS	 n: number of words to push (one byte interpreted as an integer)<br />
     * w1, w2,...wn: sequence of n words formed from pairs of bytes, the high byte appearing first<br />
     * Pushes	 w1, w2,...wn: sequence of n words each extended to 32 bits (int32)<br />
     * Related instructions	 NPUSHW[ ], PUSHB[ ]<br />
     * <p/>
     * Takes n words from the instruction stream and pushes them onto the stack.
     * <p/>
     * Looks at the next instruction stream byte n and takes n 16-bit signed words
     * <p/>
     * from the instruction stream, where n is an unsigned integer in the range
     * (0 255), and pushes them onto the stack. Each word is sign extended to 32
     * bits before being placed on the stack.The value n is not pushed onto the
     * stack.
     */
    public static final int NPUSHW = 0x41;

    /**
     * NROUND[ab] No ROUNDing of value
     * <p/>
     * Code Range	 0x6C - 0x6F <br />
     * ab	 distance type for engine characteristic compensation  <br />
     * Pops	 n1: pixel coordinate (F26Dot6) <br />
     * Pushes	 n2: pixel coordinate (F26Dot6)
     * Related instructions	 ROUND[ ]<br />
     * <p/>
     * Changes the values of the number at the top of the stack to compensate
     * for the engine characteristics.
     * <p/>
     * Pops a value, n1, from the stack and, possibly, increases or decreases
     * its value to compensate for the engine characteristics established with
     * the Boolean setting ab. The result, n2, is pushed onto the stack.
     * <p/>
     * NROUND[ab] derives its name from it relationship to ROUND[ab]. It does
     * the same operation as ROUND[ab] except that it does not round the result
     * obtained after compensating for the engine characteristics.
     */
    public static final int NROUND_0 = 0x6c;
    public static final int NROUND_1 = 0x6d;
    public static final int NROUND_2 = 0x6e;
    public static final int NROUND_3 = 0x6f;

    /**
     * ODD[] ODD
     * <p/>
     * Code Range	 0x56 <br />
     * Pops	 e1: stack element (F26Dot6) <br />
     * Pushes	 b: Boolean value<br />
     * Uses	 round state  <br />
     * Related instructions	 EVEN[ ] <br />
     * <p/>
     * Tests whether the number at the top of the stack is odd.
     * <p/>
     * Pops a number, e1, from the stack and rounds it according to the current
     * setting of the round state before testing it. The number is then truncated
     * to an integer. If the truncated number is odd, one, signifying TRUE,
     * is pushed onto the stack if it is even, zero, signifying FALSE is placed
     * onto the stack.
     */
    public static final int ODD = 0x56;

    /**
     * OR[] logical OR
     * <p/>
     * Code Range	 0x5B <br />
     * Pops	 e2: stack element e1: stack element <br />
     * Pushes	 (e1 or e2): logical or of e1 and e2 (uint32) <br />
     * Related instructions	 AND[ ] <br />
     * <p/>
     * Takes the logical or of the two numbers at the top of the stack.
     * <p/>
     * Pops two numbers, e2 and e1 off the stack and pushes onto the stack the
     * result of a logical or operation between the two elements. Zero is pushed
     * if both of the elements are FALSE (have the value zero). One is pushed if
     * either both of the elements are TRUE (has a nonzero value).
     */
    public static final int OR = 0x5b;

    /**
     * POP[] POP top stack element
     * <p/>
     * Code Range	 0x21 <br />
     * Pops	 e: stack element <br />
     * Pushes	 -  <br />
     * <p/>
     * Pops the top element from the stack.
     */
    public static final int POP = 0x21;

    /**
     * PUSHB[abc] PUSH Bytes
     * <p/>
     * de Range	 0xB0 - 0xB7  <br />
     * abc	 number of bytes to be pushed - 1   <br />
     * From IS	 b0, b1, bn: sequence of n + 1 bytes where n = 4a+2b+c = abc2  <br />
     * Pushes	 b0, b1, ,bn: sequence of n + 1 bytes each extended to 32 bits (uint32)  <br />
     * Related instructions	 NPUSHB[ ], PUSHW[ ], NPUSHB[] <br />
     * <p/>
     * Takes the specified number of bytes from the instruction stream and pushes <br />
     * them onto the interpreter stack.
     * <p/>
     * The variables a, b, and c are binary digits representing numbers from 000
     * to 111 (0-7 in binary). The value 1 is automatically added to the abc
     * figure to obtain the actual number of bytes pushed.
     * <p/>
     * When byte values are pushed onto the stack they are non-sign extended
     * with zeroes to form 32 bit numbers.
     */
    public static final int PUSHB_0 = 0xb0;
    public static final int PUSHB_1 = 0xb1;
    public static final int PUSHB_2 = 0xb2;
    public static final int PUSHB_3 = 0xb3;
    public static final int PUSHB_4 = 0xb4;
    public static final int PUSHB_5 = 0xb5;
    public static final int PUSHB_6 = 0xb6;
    public static final int PUSHB_7 = 0xb7;

    /**
     * PUSHW[abc] PUSH Words
     * <p/>
     * Code Range	 0xB8 - 0xBF  <br />
     * abc	 number of words to be pushed - 1. <br />
     * From IS	 w0,w1, wn: sequence of n+1 words formed from pairs of bytes, the high byte appearing first  <br />
     * Pushes	 w0,w1,...wn: sequence of n+1 words each padded to 32 bits (uint32)<br />
     * Related instructions	 NPUSHW[ ], PUSHB[ ]<br />
     * <p/>
     * Takes the specified number of words from the instruction stream and pushes
     * them onto the interpreter stack.
     * <p/>
     * The variables a, b, and c are binary digits representing numbers from 000
     * to 111 (0-7 binary). The value 1 is automatically added to the abc figure
     * to obtain the actual number of bytes pushed.
     * <p/>
     * When word values are pushed onto the stack they are sign extended to 32 bits.
     */
    public static final int PUSHW_0 = 0xb8;
    public static final int PUSHW_1 = 0xb9;
    public static final int PUSHW_2 = 0xba;
    public static final int PUSHW_3 = 0xbb;
    public static final int PUSHW_4 = 0xbc;
    public static final int PUSHW_5 = 0xbd;
    public static final int PUSHW_6 = 0xbe;
    public static final int PUSHW_7 = 0xbf;

    /**
     * RCVT[] Read Control Value Table entry
     * <p/>
     * Code Range	 0x45  <br />
     * Pops	 location: CVT entry number (uint32) <br />
     * Pushes	 value: CVT value (F26Dot6) <br />
     * Related instructions	 WCVTP[ ], WCVTP[ ] <br />
     * <p/>
     * Read a control value table entry and places its value onto the stack.
     * <p/>
     * Pops a CVT location from the stack and pushes the value found in the
     * location specified onto the stack.
     */
    public static final int RCVT = 0x45;

    /**
     * RDTG[] Round Down To Grid
     * <p/>
     * Code Range	 0x7D <br />
     * Pops	 -           <br />
     * Pushes	 -        <br />
     * Sets	 round state    <br />
     * Affects	 MDAP[], MDRP[], MIAP[], MIRP[], ROUND[] <br />
     * Related instructions	 RUTG[ ], RTG[], RTHG[], RTDG[], ROFF[]    <br />
     * <p/>
     * Sets the round state variable to down to grid. In this state, distances
     * are first subjected to compensation for the engine characteristics and
     * then truncated to an integer. If the result of the compensation and rounding
     * would be to change the sign of the distance, the distance is set to 0.
     */
    public static final int RDTG = 0x7D;

    /**
     * ROFF[] Round OFF
     * <p/>
     * Code Range	 0x7A  <br />
     * Pop	 -   <br />
     * Pushes	 -   <br />
     * Sets	 round state   <br />
     * Affects	 MDAP[], MDRP[], MIAP[], MIRP[], ROUND[]  <br />
     * Related instructions	 RDTG[], RUTG[ ], RTG[], RTHG[], RTDG[] <br />
     * <p/>
     * Sets the round state variable to round off. In this state engine compensation
     * occurs but no rounding takes place. If engine compensation would change
     * the sign of a distance, the distance is set to 0.
     */
    public static final int ROFF = 0x7A;

    /**
     * ROLL ROLL the top three stack elements
     * <p/>
     * Code Range 0x8A    <br />
     * Pops	 a: top stack element  <br />
     * b: second stack element from the top <br />
     * c: third stack element from the top <br />
     * Pushes	 b: second stack element <br />
     * a: top stack element    <br />
     * c: third stack element   <br />
     * Related instructions	 MINDEX[ ] <br />
     * <p/>
     * Performs a circular shift of the top three stack elements.
     * <p/>
     * Pops the top three stack elements, a, b, and c and performs a circular
     * shift of these top three objects on the stack with the effect being to
     * move the third element to the top of the stack and to move the first two
     * elements down one position. ROLL is equivalent to MINDEX[] with the value
     * 3 at the top of the stack.
     */
    public static final int ROLL = 0x8A;

    /**
     * ROUND[ab] ROUND value
     * <p/>
     * Code Range	 0x68 - 0x6B  <br />
     * Flags	 ab: distance type for engine characteristic compensation <br />
     * Pops	 n1: device space distance (F26Dot6)  <br />
     * Pushes	 n2: device space distance (F26Dot6) <br />
     * Related instructions	NROUND[ ]<br />
     * <p/>
     * Uses round state, freedom vector
     * <p/>
     * Rounds the value at the top of the stack while compensating for the engine
     * characteristics.
     * <p/>
     * Pops a 26.6 fixed point number, n1, and, depending on the engine characteristics
     * established by Booleans ab, the result is increased or decreased by a set amount.
     * The number obtained is then rounded according to the current rounding state and
     * pushed back onto the stack as n2.
     */
    public static final int ROUND_0 = 0x68;
    public static final int ROUND_1 = 0x69;
    public static final int ROUND_2 = 0x6A;
    public static final int ROUND_3 = 0x6B;

    /**
     * RS[] Read Store
     * <p/>
     * Code Range	 0x43 <br />
     * Pops	 n: storage area location (uint32)<br />
     * Pushes	 v: storage area value (uint32) <br />
     * Related instructions	 WS[ ] <br />
     * <p/>
     * Reads the value in the specified storage area location and pushes that
     * value onto the stack.
     * <p/>
     * Pops a storage area location, n, from the stack and reads a 32-bit value,
     * v, from that location. The value read is pushed onto the stack. The number
     * of available storage locations is specified in the 'maxp' table in the
     * font file'.
     */
    public static final int RS = 0x43;

    /**
     * RTDG[] Round To Double Grid
     * <p/>
     * Code Range	 0x3D <br />
     * Pops	 -   <br />
     * Pushes	 -   <br />
     * Sets	 round state  <br />
     * Affects	 MDAP[], MDRP[], MIAP[], MIRP[], ROUND[] <br />
     * Related instructions	 RDTG[], ROFF[], RUTG[ ], RTG[], RTHG[]<br />
     * <p/>
     * Sets the round state variable to double grid. In this state, distances
     * are compensated for engine characteristics and then rounded to an integer
     * or half-integer, whichever is closest.
     * <p/>
     * <b>Warning</b>
     * In TrueType, rounding is symmetric about zero and includes compensation
     * for printer dot size. See "Engine compensation using color" on page 2-65.
     */
    public static final int RTDG = 0x3d;

    /**
     * RTG[] Round To Grid
     * <p/>
     * Code Range	 0x18 <br />
     * Pops	 -      <br />
     * Pushes	 -    <br />
     * Sets	 round state   <br />
     * Affects	 MDAP[], MDRP[], MIAP[], MIRP[], ROUND[]  <br />
     * Related instructions	 RDTG[], ROFF[], RUTG[ ], RTDG[], RTHG[] <br />
     * <p/>
     * Sets the round state variable to grid. In this state, distances are
     * compensated for engine characteristics and rounded to the nearest integer.
     * <p/>
     * Warning
     * In TrueType, rounding is symmetric about zero and includes compensation
     * for printer dot size. See "Engine compensation using color" on page 2-65.
     */
    public static final int RTG = 0x18;

    /**
     * RTHG[] Round To Half Grid
     * <p/>
     * Code Range	 0x19 <br />
     * Pops	 -          <br />
     * Pushes	 -      <br />
     * Sets	 round state    <br />
     * Affects	 MDAP[], MDRP[], MIAP[], MIRP[], ROUND[]  <br />
     * Related instructions	 RDTG[], ROFF[], RUTG[ ], RTDG[], RTG[]  <br />
     * <p/>
     * Sets the round state variable to half grid. In this state, distances are
     * compensated for engine characteristics and rounded to the nearest half
     * integer. If these operations change the sign of the distance, the distance
     * is set to +1/2 or -1/2 according to the original sign of the distance.
     * <p/>
     * Warning
     * In TrueType, rounding is symmetric about zero and includes compensation
     * for printer dot size. See "Engine compensation using color" on page 2-65.
     */
    public static final int RTHG = 0x19;

    /**
     * RUTG[] Round Up To Grid
     * <p/>
     * Code Range	 0x7C <br />
     * Pops	 -  <br />
     * Pushes	 - <br />
     * Sets	 round state <br />
     * Affects	 MDAP[], MDRP[], MIAP[], MIRP[], ROUND[]<br />
     * Related instructions	 RDTG[], ROFF[], RTDG[], RTG[], RTHG[]  <br />
     * <p/>
     * Sets the round state variable to up to grid. In this state, after
     * compensation for the engine characteristics, distances are rounded up to
     * the closest integer. If the compensation and rounding would change the
     * sign of the distance, the distance will be set to 0.
     * <p/>
     * Warning
     * In TrueType, rounding is symmetric about zero and includes compensation
     * for printer dot size. See "Engine compensation using color" on page 2-65.
     */
    public static final int RUTG = 0x7c;

    /**
     * S45ROUND[] Super ROUND 45 degrees
     * <p/>
     * Code Range	 0x77
     * Pops	 n: uint32 decomposed to obtain period, phase, threshold (uint32)
     * Pushes	 -
     * Sets	 round state
     * Affects	 MDAP[], MDRP[], MIAP[], MIRP[], ROUND[]
     * Related instructions	 SROUND[ ]
     * <p/>
     * S45ROUND[ ] is analogous to SROUND[ ]. The differ is that it uses a gridPeriod
     * of pixels rather than 1 pixel. S45ROUND[ ] is useful for finely controlling
     * rounding of distances that will be measured at a 45 angle to the x-axis.
     * <p/>
     * Warning
     * In TrueType, rounding is symmetric about zero and includes compensation
     * for printer dot size. "Engine compensation using color" on page 2-65.
     * <p/>
     * Pops a number, n, from the stack and decomposes that number to obtain a
     * period, a phase and a threshold used to set the value of the graphics
     * state variable round state. Only the lower 8 bits of the argument n are
     * used to obtain these values. The byte is encoded as shown in Table 2 below.
     * <p/>
     * Table 2 SROUND[] byte encoding
     * <table border='1'>
     * <tr><td colspan='2'>period</td><td colspan='2'>phrase</td><td colspan='3'>threshold</td></tr>
     * <tr><td>7</td><td>5</td><td>4</td><td>3</td><td>2</td><td>1</td><td>0</td></tr>
     * </table>
     * <p/>
     * The next three tables give the meaning associated with the possible values for the period, phase and threshold components of n in an S45ROUND[] instruction.
     * <p/>
     * Table 3 : Setting the period
     * <table border='1'>
     * <tr><td>bit values</td><td>setting</td></tr>
     * <tr><td>00</td><td>sqr(2)/2 pixels</td></tr>
     * <tr><td>01</td><td>sqr(2) pixels</td></tr>
     * <tr><td>10</td><td>2sqr(2) pixels</td></tr>
     * <tr><td>11</td><td>Reserved</td></tr>
     * </table>
     * <p/>
     * Table 4: Setting the phase
     * <table border='1'>
     * <tr><td>bits</td><td>phase</td></tr>
     * <tr><td>00</td><td>0</td></tr>
     * <tr><td>01</td><td>period/4</td></tr>                                         1
     * <tr><td>10</td><td>period/2</td></tr>
     * <tr><td>11</td><td>period*3/4</td></tr>
     * </table>
     * <p/>
     * Table 5 : Setting the threshold
     * <table border='1'>
     * <tr><td>bits</td><td>threshold </td></tr>
     * <tr><td>0000</td><td>period -1</td></tr>
     * <tr><td>0001</td><td>-3/8 * period</td></tr>
     * <tr><td>0010</td><td>-2/8 * period</td></tr>
     * <tr><td>0011</td><td>-1/8 * period</td></tr>
     * <tr><td>0100</td><td>0/8 * period = 0 </td></tr>
     * <tr><td>0101</td><td>1/8 * period</td></tr>
     * <tr><td>0110</td><td>2/8 * period</td></tr>
     * <tr><td>0111</td><td>3/8 * period</td></tr>
     * <tr><td>1000</td><td>4/8 * period</td></tr>
     * <tr><td>1001</td><td>5/8 * period</td></tr>
     * <tr><td>1010</td><td>6/8 * period</td></tr>
     * <tr><td>1011</td><td>7/8 * period </td></tr>
     * <tr><td>1100</td><td>8/8 * period = period </td></tr>
     * <tr><td>1101</td><td>9/8 * period</td></tr>
     * <tr><td>1110</td><td>10/8 * period</td></tr>
     * <tr><td>1111</td><td>11/8 * period </td></tr>
     * </table>
     */
    public static final int S45ROUND = 0x77;

    /**
     * SANGW[] Set Angle Weight
     * <p/>
     * Code Range	 0x7E <br />
     * Pops	 weight: value for angle weight (uint32)<br />
     * Pushes	 -<br />
     * Sets	 angle weight<br />
     * Related	 instructions AA[ ]<br />
     * <p/>
     * Pops a 32 bit integer, weight, from the stack and sets the value of the
     * angle weight state variable accordingly. This instruction is anachronistic.
     * Except for popping a single stack element, it has no effect.
     */
    public static final int SANGW = 0x7e;

    /**
     * SCANCTRL[] SCAN conversion ConTRoLht
     * <p/>
     * de Range	 0x85   <br />
     * Pops	 n: flags indicating when to turn on dropout control mode  <br />
     * Pushes	 -  <br />
     * Sets	 scan control  <br />
     * Related instructions	 SCANTYPE[ ]    <br />
     * <p/>
     * Pops a number, n, which is decomposed to a set of flags specifying the
     * dropout control mode. SCANCTRL is used to set the value of the graphics
     * state variable scan control which in turn determines whether the scan
     * converter will activate dropout control for this glyph. Use of the dropout
     * control mode is determined by three conditions:
     * <ul>
     * <li>Is the glyph rotated?</li>
     * <li>Is the glyph stretched?</li>
     * <li>Is the current setting for ppem less than a specified threshold?</li>
     * </ul>
     * The interpreter pops a word from the stack and looks at the lower 13 bits.
     * <p/>
     * Bits 0-7 represent the threshold value for ppem. In conjunction with
     * bit 8, a value of FF in bits 0-7 means invoke dropout control for all
     * sizes. Sia value of 15 in bits 0-7 means invoke dropout control below
     * 16 pixels per em. Note that 0xFE or 254 is the largest number of pixels
     * per em for which dropout control can be selectively invoked.
     * <p/>
     * Bits 8-13 are used to specify when to dropout control. Bits 8, 9 and 10
     * are used to turn on the dropout control mode (assuming other conditions
     * do not block it). Bits 11, 12, and 13 are used to turn off the dropout
     * mode unless other conditions force it.
     * <p/>
     * Bit	 Meaning If set
     * 8 -  Set dropout control to TRUE if other conditions do not block and ppem is less than or equal to the threshold value<br />
     * 9 - Set dropout control to TRUE if other conditions do not block and the glyph is rotated  <br />
     * 10 -  Set dropout control to TRUE if other conditions do not block and the glyph is stretched.<br />
     * 11 - Set dropout control to FALSE unless ppem is less than or equal to the threshold value. <br />
     * 12 - Set dropout control to FALSE unless the glyph is rotated. <br />
     * 13 -  Set dropout control to FALSE unless the glyph is stretched <br />
     * <p/>
     * For example, the values given below have the effect stated. <br />
     * 0x0 -  No dropout control is invoked   <br />
     * 0x1FF - Always do dropout control   <br />
     * 0xA10 - Do dropout control if the glyph is rotated and has less than 16 pixels per em  <br />
     * <p/>
     * The scan converter can operate in either a "normal" mode or in a "fix dropout"
     * mode depending on the value of a set of enabling and disabling flags.
     */
    public static final int SCANCTRL = 0x85;

    /**
     * SCANTYPE[] SCANTYPE
     * <p/>
     * Code Range	 0x8D<br />
     * Pops	 n: stack element<br />
     * Pushes	 -  <br />
     * Sets	 scan_control <br />
     * Related instructions	 SCANCTRL[ ] <br />
     * <p/>
     * Used to choose between dropout control with subs and without stubs.
     * <p/>
     * Pops a stack element consisting of a16-bit integer extended to 32 bits.
     * The value of this integer is used to determine which rules the scan converter
     * will use. If the value of the argument is 2, the non-dropout control scan
     * converter will be used. If the value of the integer is 0 or 1, the dropout
     * control mode will be set. More specifically,
     * <p/>
     * if n=0 rules 1 and 2 are invoked (dropout control scan conversion including stubs)
     * <p/>
     * if n=1 rules 1 and 3 are invoked (dropout control scan conversion excluding stubs)
     * <p/>
     * if n=2 rule 1 is invoked (fast scan conversion)
     * <p/>
     * The scan conversion rules are shown here:
     * <p/>
     * Rule 1<br />
     * If a pixel's center falls within or on the glyph outline, that pixel is
     * turned on and becomes part of that glyph.
     * <p/>
     * Rule 2 <br />
     * If a scan line between two adjacent pixel centers (either vertical or horizontal)
     * is intersected by both an on-Transition contour and an off-Transition contour and
     * neither of the pixels was already turned on by rule 1, turn on the left-most pixel
     * (horizontal scan line) or the bottom-most pixel (vertical scan line)
     * <p/>
     * Rule 3 <br />
     * Apply Rule 2 only if the two contours continue to intersect other scan lines
     * in both directions. That is, do not turn on pixels for 'stubs'. The scanline
     * segments that form a square with the intersected scan line segment are examined
     * to verify that they are intersected by two contours. It is possible that these
     * could be different contours than the ones intersecting the dropout scan line
     * segment. This is very unlikely but may have to be controlled with
     * grid-fitting in some exotic glyphs.
     * <p/>
     * Return to Contents
     */
    public static final int SCANTYPE = 0x8d;

    /**
     * SCFS[] Sets Coordinate From the Stack using projection vector and freedom vector
     * <p/>
     * Code Range	 0x48 <br />
     * Pops	 c: coordinate value (F26Dot6)<br />
     * p: point number (uint32)<br />
     * Pushes	 -   <br />
     * Uses	 zp2, freedom vector, projection vector  <br />
     * Related instructions	 GC[ ] <br />
     * <p/>
     * Moves a point to the position specified by the coordinate value given
     * on the stack.
     * <p/>
     * Pops a coordinate value, c, and a point number, p, and moves point p from
     * its current position along the freedom vector so that its component along
     * the projection vector becomes the value popped off the stack.
     * <p/>
     * This instruction can be used to "create" points in the twilight zone.
     * <p/>
     * In the illustration below, point p is moved along the freedom vector until
     * its coordinate on the projection vector has the value c.
     */
    public static final int SCFS = 0x48;

    /**
     * SCVTCI[] Set Control Value Table Cut-In
     * <p/>
     * Code Range	 0x1D   <br />
     * Pops	 n: value for cut-in (F26Dot6)   <br />
     * Pushes	 -   <br />
     * Sets	 control value cut-in  <br />
     * Affects	 MIAP, MIRP  <br />
     * <p/>
     * Establish a new value for the control value table cut-in.
     * <p/>
     * Pops a value, n, from the stack and sets the control value cut-in to n.
     * Increasing the value of the cut-in will increase the range of sizes for
     * which CVT values will be used instead of the original outline value.
     */
    public static final int SCVTCI = 0x1d;

    /**
     * SDB[] Set Delta Base in the graphics state
     * <p/>
     * Code Range	 0x5E <br />
     * Pops	 n: value for the delta base (uint32) <br />
     * Pushes	 - <br />
     * Sets	 delta base  <br />
     * Affects	 DELTAP1[], DELTAP2[], DELTAP3[], DELTAC1[], DELTAC2[], DELTAC3[]  <br />
     * Related instructions	 SDS[ ]<br />
     * <p/>
     * Establishes a new value for the delta base state variable thereby changing
     * the range of values over which a DELTA[] instruction will have an affect.
     * <p/>
     * Pops a number, n, and sets delta base to the value n. The default for delta base is 9.
     */
    public static final int SDB = 0x5e;

    /**
     * SDPVTL[a] Set Dual Projection Vector To Line
     * <p/>
     * Code Range	 0x86 - 0x87 <br />
     * a	 0: Vector is parallel to line<br />
     * 1: Vector is perpendicular to line<br />
     * Pops	 p2: point number (uint32)<br />
     * p1: point number (uint32)<br />
     * Pushes	 - <br />
     * Sets	 dual projection vector, projection vector, zp2 with p2, zp1 with p1 <br />
     * Related instructions	 SPVTL[ ]<br />
     * <p/>
     * Sets a second projection vector based upon the original position of two
     * points. The new vector will point in a direction that is parallel to the
     * line defined from p2 to p1. The projection vector is also set in in a
     * direction that is parallel to the line from p2 to p1 but it is set using
     * the current position of those points.
     * <p/>
     * Pops two point numbers from the stack and uses them to specify a line that
     * defines a second, dual projection vector. This dual projection vector uses
     * coordinates from the original outline before any instructions are executed.
     * It is used only with the IP[], GC[], MD[], MDRP[] and MIRP[] instructions.
     * The dual projection vector is used in place of the projection vector in
     * these instructions. This continues until some instruction sets the
     * projection vector again.
     */
    public static final int SDPVTL_0 = 0x86;
    public static final int SDPVTL_1 = 0x87;

    /**
     * SDS[] Set Delta Shift in the graphics state
     * <p/>
     * Code Range	 0x5F <br />
     * Pops	 n: value for the delta shift (uint32)<br />
     * Pushes	 - <br />
     * Sets	 delta shift <br />
     * Affects	 DELTAP1[], DELTAP2[], DELTAP3[], DELTAC1[], DELTAC2[], DELTAC3[] <br />
     * Related instructions	 SDB[ ] <br />
     * <p/>
     * Establish a new value for the delta shift state variable thereby changing
     * the step size of the DELTA[] instructions.
     * <p/>
     * Pops a value n from the stack and sets delta shift to n. The default for
     * delta shift is 3.
     */
    public static final int SDS = 0x5f;

    /**
     * SFVFS[] Set Freedom Vector From Stack
     * <p/>
     * Code	 0x0B <br />
     * Pops	 y: y component of freedom vector (F2Dot14) <br />
     * x: x component of freedom vector (F2Dot14)<br />
     * Pushes	 -  <br />
     * Sets	 freedom vector <br />
     * Related instructions	 SFVTL[ ], SFVTPV[ ], SFVTCA[ ] <br />
     * <p/>
     * Changes the direction of the freedom vector using values take from the
     * stack and thereby changing the direction in which points can move.
     * <p/>
     * Sets the direction of the freedom vector using the values x and y taken
     * from the stack. The vector is set so that its projections onto the x and
     * y -axes are x and y, which are specified as signed (two's complement)
     * fixed-point (2.14) numbers. The value (x2 + y2) must be equal to 1 (0x4000).
     */
    public static final int SFVFS = 0x0b;

    /**
     * SFVTCA[a] Set Freedom Vector To Coordinate Axis
     * <p/>
     * Code range	 0x04 - 0x05  <br />
     * a	 0: set the freedom vector to the y-axis <br />
     * 1: set the freedom vector to the x-axis <br />
     * Pops	 - <br />
     * Pushes	 -  <br />
     * Sets	 freedom vector <br />
     * Related instructions	 SFVFS[ ], SFVTL[ ], SFVTPV[ ] <br />
     * <p/>
     * Sets the freedom vector to one of the coordinate axes depending upon the
     * value of the flag a.
     */
    public static final int SFVTCA_0 = 0x04;
    public static final int SFVTCA_1 = 0x05;

    /**
     * SFVTL[a] Set Freedom Vector To Line
     * <p/>
     * 0x08 - 0x09  <br />
     * a	 0: set freedom vector to be parallel to the line segment defined by
     * points p1 and p2 <br />
     * 1: set freedom vector perpendicular to the line segment defined by points
     * p1 and p2; the vector is rotated counter clockwise 90 degrees <br />
     * Pops	 p2: point number (uint32)  <br />
     * p1: point number (uint32) <br />
     * Pushes	 -  <br />
     * Sets	 freedom vector   <br />
     * Uses	 zp1 points to the zone containing point p1 zp2 points to the zone containing point p2  <br />
     * Related instructions	 SFVTPV[ ], SFVFS[ ], SFVTCA[ ]  <br />
     * <p/>
     * Change the value of the freedom vector using the direction specified by
     * the line whose end points are taken from the stack. The effect is to change
     * the direction in which points can move to be parallel to that line. The
     * order in which the points are chosen is significant. Reversing the order
     * will reverse the direction of the freedom vector.
     * <p/>
     * Pops two point numbers p2 and p1 from the stack and sets the freedom
     * vector to a unit vector parallel or perpendicular to the line segment
     * defined by points p1 and p2 and pointing from p2 to p1.
     * <p/>
     * If the Boolean a has the value 0, the freedom vector is parallel to the
     * line from p2 to p1.
     * <p/>
     * If the Boolean a has the value one, the freedom vector is perpendicular
     * to the line from p2 to p1. More precisely, the freedom vector is obtained
     * by rotating the vector that is parallel to the line 90 counter clockwise.
     */
    public static final int SFVTL_0 = 0x08;
    public static final int SFVTL_1 = 0x09;

    /**
     * SFVTPV[] Set Freedom Vector To Projection Vector
     * <p/>
     * Code	 0x0E <br />
     * Pops	 -    <br />
     * Pushes	 -   <br />
     * Sets	 freedom vector<br />
     * Related instructions	 SFVFS[ ], SFVTL[ ], SFVTCA[ ] <br />
     * <p/>
     * Sets the freedom vector to be the same as the projection vector. This
     * means that movement and measurement will be in the same direction.
     */
    public static final int SFVTPV = 0x0e;

    /**
     * SHC[a] SHift Contour using reference point
     * <p/>
     * Code Range	 0x34 - 0x35  <br />
     * a	 0: uses rp2 in the zone pointed to by zp1   <br />
     * 1: uses rp1 in the zone pointed to by zp0  <br />
     * Pops	 c: contour to be shifted (uint32) <br />
     * Pushes	 -  <br />
     * Uses	 zp0 with rp1 or zp1 with rp2 depending on flag zp2 with contour c freedom vector, projection vector   <br />
     * Related instructions	 SHP[ ], SHZ[ ]<br />
     * <p/>
     * Shifts a contour by the amount that the reference point was shifted.
     * <p/>
     * Pops a number, c, and shifts every point on contour c by the same amount
     * that the reference point has been shifted. Each point is shifted along the
     * freedom vector so that the distance between the new position of the point
     * and the old position of that point is the same as the distance between the
     * current position of the reference point and the original position of the
     * reference point. The distance is measured along the projection vector. If
     * the reference point is one of the points defining the contour, the reference
     * point is not moved by this instruction.
     * <p/>
     * This instruction is similar to SHP[], but every point on the contour is shifted.
     * <p/>
     * In the illustration below, the triangular contour formed by points ,, and is
     * shifted by the amount, d, that reference point rp was moved from its original
     * position. The new contour p1, p2, p3 retains the original shape but has been
     * translated in space, along the freedom vector by the amount, d.
     */
    public static final int SHC_0 = 0x34;
    public static final int SHC_1 = 0x35;

    /**
     * SHP[a] SHift Point using reference point
     * <p/>
     * Code Range	 0x32 - 0x33
     * a	 0: uses rp2 in the zone pointed to by zp1
     * 1: uses rp1 in the zone pointed to by zp0
     * Pops	 p1, p2, , ploopvalue: point to be shifted (uint32)
     * Pushes	 -
     * Uses	 zp0 with rp1 or zp1 with rp2 depending on flag zp2 with point p loop, freedom vector, projection vector
     * <p/>
     * Shifts points specified by the amount the reference point has already been shifted.
     * <p/>
     * Pops point numbers, p1, p2, , ploopvalue, and shifts those points by the same
     * amount that the reference point has been shifted. Each point pi is moved along
     * the freedom vector so that the distance between the new position of point pi
     * and the current position of point pi is the same as the distance between the
     * current position of the reference point and the original position of the
     * reference point.
     * <p/>
     * In the illustration below, the distance between the current position of the
     * reference point and its original position is d. Line LL' is drawn perpendicular
     * to the projection vector at a distance d from point A'. Point p is moved along
     * the freedom vector to the point where the vector intersects with line LL'. The
     * distance from point A' to B', d, is now the same as the distance from A to B.
     */
    public static final int SHP_0 = 0x32;
    public static final int SHP_1 = 0x33;

    /**
     * SHPIX[] SHift point by a PIXel amount
     * <p/>
     * Code Range	 0x38<br />
     * Pops	 d: magnitude of the shift (F26Dot6)<br />
     * p1, p2, , ploopvalue: point to be shifted (uint32) <br />
     * Pushes	 -  <br />
     * Uses	 zp2, loop, freedom vector  <br />
     * Related instructions	 SHP[ ] <br />
     * <p/>
     * Shift the specified points by the specified amount.
     * <p/>
     * Pops point numbers p1, p2, , ploopvalue and an amount. Shifts each point
     * pi by amount d.
     * <p/>
     * SHPIX[ ] is unique in relying solely on the direction of the freedom vector
     * It makes no use of the projection vector. Measurement is made in the
     * direction of the freedom vector.
     * <p/>
     * In the example below, point p is moved d pixels along the freedom vector.
     */
    public static final int SHPIX = 0x38;

    /**
     * SHZ[a] SHift Zone using reference point
     * <p/>
     * Code Range	 0x36 - 0x37 <br />
     * a 0: the reference point rp2 is in the zone pointed to by zp1 <br />
     * 1: the reference point rp1 is in the zone pointed to by zp0 <br />
     * Pops	 e: zone to be shifted (uint32) <br />
     * Pushes	 - <br />
     * Uses	 zp0 with rp1 or zp1 with rp2 depending on flag freedom vector, projection vector<br />
     * Related instructions	 SHP[ ], SHC[ ] <br />
     * <p/>
     * Shifts all of the points in the specified zone by the amount that the
     * reference point has been shifted.
     * <p/>
     * Pops a zone number, e, and shifts the points in the specified zone
     * (Z1 or Z0) by the same amount that the reference point has been shifted.
     * The points in the zone are shifted so that the distance between the new
     * position of the shifted points and their old position is the same as the
     * distance between the current position of the reference point and the
     * original position of the reference point.
     * <p/>
     * SHZ[a] uses zp0 with rp1 or zp1 with rp2. This instruction is similar to
     * SHC[ ], but all points in the zone are shifted, not just the points on a
     * single contour.
     */
    public static final int SHZ_0 = 0x36;
    public static final int SHZ_1 = 0x37;

    /**
     * SLOOP[] Set LOOP variable
     * <p/>
     * Code Range	 0x17 <br />
     * Pops	 n: value for loop graphics state variable (integer) <br />
     * Pushes	 - <br />
     * Sets	 loop<br />
     * Affects	 ALIGNRP[], FLIPPT[], IP[], SHP[], SHPIX[] <br />
     * Related instructions	 LOOPCALL[ ] <br />
     * <p/>
     * Changes the value of the loop variable thereby changing the number of
     * times the affected instructions will execute if called.
     * <p/>
     * Pops a value, n, from the stack and sets the loop variable count to that
     * value. The loop variable works with the SHP[a], SHPIX[a], IP[ ], and
     * ALIGNRP[]. The value n indicates the number of times the instruction is
     * to be repeated. After the instruction executes the required number of
     * times, the loop variable is reset to its default value of 1. Setting the
     * loop variable to zero is an error.
     */
    public static final int SLOOP = 0x17;

    /**
     * SMD[] Set Minimum Distance
     * <p/>
     * Code Range	 0x1A     <br />
     * Pops	 distance: value for minimum_distance (F26Dot6)    <br />
     * Pushes	 -   <br />
     * Sets	 minimum distance <br />
     * <p/>
     * Establishes a new value for the minimum distance, the smallest possible
     * value to which distances will be rounded. An appropriate setting for this
     * variable can prevent distances from rounding to zero and therefore disappearing
     * when grid-fitting takes place.
     * <p/>
     * Pops a 26.6 value from the stack and sets the minimum distance variable to
     * that value.
     */
    public static final int SMD = 0x1a;

    /**
     * SPVFS[] Set Projection Vector From Stack
     * <p/>
     * Code Range	 0x0A <br />
     * Pops	 y: y component of projection vector (F2Dot14) x: x component of projection vector (F2Dot14)  <br />
     * Pushes	 - <br />
     * Sets	 projection vector   <br />
     * Related instructions	 SPVTL[ ], SPVTCA[ ]  <br />
     * <p/>
     * Establishes a new value for the projection vector using values taken from the stack.
     * <p/>
     * Pops two numbers y and x representing the y an x components of the projection
     * vector. The values x and y are 2.14 numbers extended to 32 bits. Sets the
     * direction of the projection vector, using values x and y taken from the
     * stack, so that its projections onto the x and y-axes are x and y, which
     * are specified as signed (two's complement) fixed-point (2.14) numbers. The
     * value (x2 + y2) must be equal to 1 (0x4000).
     */
    public static final int SPVFS = 0x0a;

    /**
     * SPVTCA[a] Set Projection Vector To Coordinate Axis
     * <p/>
     * Code range	 0x02 - 0x03<br />
     * a	 0: set the projection vector to the y-axis <br />
     * 1: set the projection vector to the x-axis  <br />
     * Pops	 - <br />
     * Pushes	 -  <br />
     * Sets	 projection vector   <br />
     * Related instructions	 SPVTL[ ], SPVFS[ ]  <br />
     * <p/>
     * Sets the projection vector to one of the coordinate axes depending on the value of the flag a.
     */
    public static final int SPVTCA_0 = 0x02;
    public static final int SPVTCA_1 = 0x03;

    /**
     * SPVTL[a] Set Projection Vector To Line
     * <p/>
     * Code Range	 0x06 - 0x07 <br />
     * a	 0: sets projection vector to be parallel to line segment from p2 to p1  <br />
     * 1: sets projection vector to be perpendicular to line segment from p2 to p1; the
     * vector is rotated counter clockwise 90 degrees  <br />
     * Pops	 p2: point number (uint32)  <br />
     * p1: point number (uint32)    <br />
     * Pushes	 - <br />
     * Uses	 point p1 in the zone pointed at by zp1 point p2 in the zone pointed at by zp2 <br />
     * Sets	 projection vector<br />
     * Related instructions	 SPVFS[ ], SPVTCA[ ] <br />
     * <p/>
     * Changes the direction of the projection vector to that specified by the line
     * defined by the endpoints taken from the stack. The order in which the points
     * are specified is significant Reversing the order of the points will reverse
     * the direction of the projection vector.
     * <p/>
     * Pops two point numbers, p2 and p1 and sets the projection vector to a unit
     * vector parallel or perpendicular to the line segment from point p2 to point
     * p1 and pointing from p2 to p1.
     */
    public static final int SPVTL_0 = 0x06;
    public static final int SPVTL_1 = 0x07;

    /**
     * SROUND[] Super ROUND
     * <p/>
     * Code Range	 0x76 <br />
     * Pops	 n: number decomposed to obtain period, phase, threshold (Eint8)<br />
     * Pushes	 -  <br />
     * Sets	 round state <br />
     * Affects	 MDAP[], MDRP[], MIAP[], MIRP[], ROUND[] <br />
     * Related instructions	 S45ROUND[ ]  <br />
     * <p/>
     * Provides for fine control over the effects of the round state variable by
     * directly setting the values of the three components of the round state:
     * period, phase, and threshold.
     * <p/>
     * Pops a number, n, from the stack and decomposes that number to obtain a
     * period, a phase and a threshold used to set the value of the graphics state
     * variable round state. Only the lower 8 bits of the argument n are used to
     * obtain these values. The byte is encoded as shown in Table 8 below.
     * <p/>
     * Table 8: SROUND byte encoding
     * <table border='1'>
     * <tr><td colspan='2'>period</td><td colspan='2'>phase</td><td colspan='3'>threshold</td></tr>
     * <tr><td>7</td><td>6</td><td>5</td><td>4</td><td>3</td><td>2</td><td>1</td></tr>
     * </table>
     * The period specifies the length of the separation or space between rounded
     * values. The phase specifies the offset of the rounded values from multiples
     * of the period. The threshold specifies the part of the domain, prior to a
     * potential rounded value, that is mapped onto that value.Additional information
     * on rounding can be found in "Rounding" on page 2-66.
     * <p/>
     * For SROUND[] the grid period used to compute the period shown in Table 9
     * is equal to 1.0 pixels. Table 10 lists the possible values for the phase
     * and Table 11 the possible values for the threshold.,
     * Table 9: Setting the period
     * <table border='1'>
     * <tr><td>bit value</td><td>setting</td></tr>
     * <tr><td>00</td><td>1/2 pixel </td></tr>
     * <tr><td>01</td><td>1 pixel </td></tr>
     * <tr><td>10</td><td>2 pixel </td></tr>
     * <tr><td>11</td><td>Reserved</td></tr>
     * </table>
     * Table 10: Setting the phase
     * <table border='1'>
     * <tr><td>bit value</td><td>setting</td></tr>
     * <tr><td>00</td><td>0</td></tr>
     * <tr><td>01</td><td>period/4 </td></tr>
     * <tr><td>10</td><td>period/2 </td></tr>
     * <tr><td>11</td><td>period*3/4 </td></tr>
     * </table>
     * Table 11: Setting the threshold
     * <table border='1'>
     * <tr><td>bit value</td><td>setting</td></tr>
     * <tr><td>0000</td><td>period -1 </td></tr>
     * <tr><td>0001</td><td>-3/8 * period</td></tr>
     * <tr><td>0010</td><td>-2/8 * period</td></tr>
     * <tr><td>0011</td><td>-1/8 * period</td></tr>
     * <tr><td>0100</td><td>0/8 * period = 0</td></tr>
     * <tr><td>0101</td><td>1/8 * period</td></tr>
     * <tr><td>0110</td><td>2/8 * period</td></tr>
     * <tr><td>0111</td><td>3/8 * period </td></tr>
     * <tr><td>1000</td><td>4/8 * period </td></tr>
     * <tr><td>1001</td><td>5/8 * period</td></tr>
     * <tr><td>1010</td><td>6/8 * period </td></tr>
     * <tr><td>1011</td><td>7/8 * period </td></tr>
     * <tr><td>1100</td><td>8/8 * period = period </td></tr>
     * <tr><td>1101</td><td>9/8 * period  </td></tr>
     * <tr><td>1110</td><td>10/8 * period </td></tr>
     * <tr><td>1111</td><td>11/8 * period  </td></tr>
     * </table>
     */
    public static final int SROUND = 0x76;

    /**
     * SRP0[] Set Reference Point 0
     * <p/>
     * Code Range	 0x10 <br />
     * Pops	 p: point number (uint32)<br />
     * Pushes	 -<br />
     * Sets	 rp0<br />
     * Affects	 ALIGNRP[], MDAP[], MDRP[], MIAP[], MIRP[] MSIRP[]<br />
     * Related instructions	 SRP1[ ], SRP2[ ]<br />
     * <p/>
     * Sets a new value for reference point 0.
     * <p/>
     * Pops a point number, p, from the stack and sets rp0 to p.
     */
    public static final int SRP0 = 0x10;

    /**
     * SRP1[] Set Reference Point 1
     * <p/>
     * Code Range	 0x11<br />
     * Pops	 p: point number (uint32)<br />
     * Pushes	 - <br />
     * Sets	 rp1 <br />
     * Affects	 IP[], MDAP[], MIAP[], MSIRP[], SHC[], SHP[], SHZ <br />
     * Related instructions	 SRP0[], SRP2[ ]<br />
     * <p/>
     * Sets a new value for reference point 1.
     * <p/>
     * Pops a point number, p, from the stack and sets rp1 to p.
     */
    public static final int SRP1 = 0x11;

    /**
     * SRP2[] Set Reference Point 2
     * <p/>
     * Code Range	 0x12<br />
     * Pops	 p: point number (uint32)<br />
     * Pushes	 -<br />
     * Sets	 rp2<br />
     * Affects	 IP[], SHC[], SHP[], SHZ[]<br />
     * Related instructions	 SRP1[ ], SRP0[]<br />
     * <p/>
     * Sets a new value for reference point 2.
     * <p/>
     * Pops a point number, p, from the stack and sets rp2 to p.
     */
    public static final int SRP2 = 0x12;

    /**
     * SSW[] Set Single Width
     * <p/>
     * Code Range	 0x1F  <br />
     * Pops	 n: value for single width value (FUnit)  <br />
     * Pushes	 - <br />
     * Sets	 single width value  <br />
     * Related instructions	 SSWCI[ ] <br />
     * <p/>
     * Establishes a new value for the single width value state variable. The
     * single width value is used instead of a control value table entry when
     * the difference between the single width value and the given CVT entry is
     * less than the single width cut-in.
     * <p/>
     * Pops a 32 bit integer value, n, from the stack and sets the single width
     * value in the graphics state to n. The value n is expressed in FUnits.
     */
    public static final int SSW = 0x1f;

    /**
     * SSWCI[] Set Single Width Cut-In
     * <p/>
     * Code Range	 0x1E  <br />
     * Pops	 n: value for single width cut-in (F26Dot6)<br />
     * Pushes	 -<br />
     * Sets	 single width cut-in <br />
     * Affects	 MIAP[], MIRP[]<br />
     * Related instructions	 SSW[ ] <br />
     * <p/>
     * Establishes a new value for the single width cut-in, the distance difference
     * at which the interpreter will ignore the values in the control value table
     * in favor of the single width value.
     * <p/>
     * Pops a 32 bit integer value, n, and sets the single width cut-in to n.
     */
    public static final int SSWCI = 0x1e;

    /**
     * SUB[] SUBtract
     * <p/>
     * Code Range	 0x61  <br />
     * Pops	 n2: subtrahend (F26Dot6)  <br />
     * n1: minuend (F26Dot6)   <br />
     * Pushes	 (n1 - n2): difference (F26Dot6)    <br />
     * Related instructions	 ADD[ ]  <br />
     * Subtracts the number at the top of the stack from the number below it. <br />
     * <p/>
     * Pops two 26.6 numbers, n1 and n2, from the stack and pushes the difference
     * between the two elements onto the stack.
     */
    public static final int SUB = 0x61;

    /**
     * SVTCA[a] Set freedom and projection Vectors To Coordinate Axis
     * <p/>
     * Code range	 0x00 - 0x01   <br />
     * a	 0: set vectors to the y-axis <br />
     * 1: set vectors to the x-axis<br />
     * Pops	 -<br />
     * Pushes	 -<br />
     * Sets	 projection vector, freedom vector<br />
     * Related instructions	 SPTCA[ ], SFVTCA[ ]<br />
     * <p/>
     * Sets both the projection vector and freedom vector to the same coordinate
     * axis causing movement and measurement to be in the same direction. The
     * setting of the Boolean variable a determines the choice of axis.
     * <p/>
     * SVTCA[ ] is a shortcut that replaces the SFVTCA[ ] and SPVTCA[ ] instructions.
     * As a result, SVTCA[1] is equivalent to SFVTCA[1] followed by SPVTCA[1].
     */
    public static final int SVTCA_0 = 0x00;
    public static final int SVTCA_1 = 0x01;

    /**
     * SWAP[] SWAP the top two elements on the stack
     * <p/>
     * Code Range	 0x23<br />
     * Pops	 e2: stack element (StkElt) <br />
     * e1: stack element (StkElt)<br />
     * Pushes	 e2: stack element (StkElt) <br />
     * e1: stack element (StkElt) <br />
     * <p/>
     * Swaps the top two stack elements.
     * <p/>
     * Pops two elements, e2 and e1, from the stack and reverses their order
     * making the old top element the second from the top and the old second
     * element the top element.
     */
    public static final int SWAP = 0x23;

    /**
     * SZP0[] Set Zone Pointer 0
     * <p/>
     * Code Range	 0x13 <br />
     * Pops	 n: zone number (uint32)<br />
     * Pushes	 -<br />
     * Sets	 zp0<br />
     * Affects	 AA[], ALIGNPTS[], ALIGNRP[], DELTAC1[], DELTAC2[], DELTAC3[], DELTAP1[], DELTAP2[], DELTAP3[], FLIPPT[], FLIPRGOFF[], FLIPRGON[], IP[], ISECT[], MD[], MDAP[], MDRP[], MIAP[], MIRP[], MSIRP[], SHC[], SHE[], SHP[], SHZ[], UTP[]<br />
     * Related instructions	 SZP1[ ], SZP2[ ], SZPS[ ]<br />
     * <p/>
     * Establishes a new value for zp0. It can point to either the glyph zone
     * or the twilight zone.
     * <p/>
     * Pops a zone number, n, from the stack and sets zp0 to the zone with that
     * number. If n has the value zero, zp0 points to zone 0 (the twilight zone).
     * If n has the value one, zp0 points to zone 1 (the glyph zone). Any other
     * value for n is an error.
     */
    public static final int SZP0 = 0x13;

    /**
     * SZP1[] Set Zone Pointer 1
     * <p/>
     * Code Range	 0x14 <br />
     * Pops	 n: zone number (uint32) <br />
     * Pushes	 -     <br />
     * Sets	 zp1  <br />
     * Affects	 ALIGNPTS[], ALIGNRP[], IP[], ISECT[], MD[], MDRP[], MIRP[], MSIRP[], SDPVTL[], SFVTL[], SHC[], SHP[], SHZ[], SPVTL[] <br />
     * Related instructions	 SZP0[ ], SZP2[ ], SZPS[ ]<br />
     * <p/>
     * Establishes a new value for zp1. It can point to either the glyph zone or
     * the twilight zone.
     * <p/>
     * Pops a zone number, n, from the stack and sets zp1 to the zone with that number.
     * If n has the value zero, zp1 points to zone 0 (the twilight zone). If n
     * has the value one, zp1 points to zone 1 (the glyph zone). Any other value
     * for n is an error.
     */
    public static final int SZP1 = 0x14;

    /**
     * SZP2[] Set Zone Pointer 2
     * <p/>
     * Code Range	 0x15  <br />
     * Pops	 n: zone number (uint32) <br />
     * Pushes	 - <br />
     * Sets	 zp2 <br />
     * Affects	 IP[], ISECT[], IUP[], GC[], SDPVTL[], SHC[], SHP[], SFVTL[], SHPIX[], SPVTL[], SC[]  <br />
     * Related instructions	 SZP0[ ], SZP1[ ], SZPS[ ] <br />
     * <p/>
     * Establishes a new value for zp2. It can point to either the glyph zone or
     * the twilight zone.
     * <p/>
     * Pops a zone number, n, from the stack and sets zp2 to the zone with that
     * number. If n has the value zero, zp2 points to zone 0 (the twilight zone).
     * If n has the value one, zp2 points to zone 1 (the glyph zone). Any other
     * value for n is an error.
     */
    public static final int SZP2 = 0x15;

    /**
     * SZPS[] Set Zone PointerS
     * <p/>
     * Code Range	 0x16  <br />
     * Pops	 n: zone number (uint32)<br />
     * Pushes	 - <br />
     * Sets	 zp0, zp1, zp2 <br />
     * Affects	 ALIGNPTS[], ALIGNRP[], DELTAC1[], DELTAC2[], DELTAC3[], DELTAP1[], DELTAP2[], DELTAP3[], FLIPPT[], FLIPRGOFF[], FLIPRGON[], GC[], IP[], ISECT[], IUP[], MD[], MDAP[], MDRP[], MIAP[], MIRP[], MSIRP[], SC[], SDPVTL[], SFVTL[], SHPIX[], SPVTL[], SHC[], SHP[], SHZ[], SPVTL[], UTP[]<br />
     * Related instructions	 SZP0[ ], SZP1[ ], SZP2[ ]<br />
     * <p/>
     * Sets all three zone pointers to refer to either the glyph zone or the twilight zone.
     * <p/>
     * Pops an integer n from the stack and sets all of the zone pointers to point to the zone with that number. If n is 0, all three zone pointers will point to zone 0 (the twilight zone). If n is 1, all three zone pointers will point to zone 1 (the glyph zone). Any other value for n is an error.
     */
    public static final int SZPS = 0x16;

    /**
     * UTP[] UnTouch Point
     * <p/>
     * Code Range	 0x29<br />
     * Pops	 p: point number (uint32) <br />
     * Pushes	 - <br />
     * Uses	 zp0 with point p, freedom vector <br />
     * Affects	 IUP[ ] <br />
     * <p/>
     * Marks a point as untouched thereby causing the IUP[ ] instruction to affect its location.
     * <p/>
     * Pops a point number, p, and marks point p as untouched. A point may be
     * touched in the x-direction, the y-direction, or in both the x and
     * y-directions. The position of the freedom vector determines whether the
     * point is untouched in the x-direction, the y-direction, or both. If the
     * vector is set to the x-axis, the point will be untouched in the x-direction.
     * If the vector is set to the y-axis, the point will be untouched in the
     * y-direction. Otherwise the point will be untouched in both directions.
     * <p/>
     * A points that is marked as untouched will be moved by an IUP[ ]
     * instruction even if the point was previously touched.
     */
    public static final int UTP = 0x29;

    /**
     * WCVTF[] Write Control Value Table in Funits
     * <p/>
     * Code Range	 0x70  <br />
     * Pops	 n: number in FUnits (uint32) l: control value table location (uint32)  <br />
     * Pushes	 -  <br />
     * Sets	 control value table entry  <br />
     * Related instructions	 WCVTP[ ]  <br />
     * <p/>
     * Writes a scaled F26Dot6 value to the specified control value table location.
     * <p/>
     * Pops an integer value, n, and a control value table location l from the stack.
     * The FUnit value is scaled to the current point size and resolution and put
     * in the control value table. This instruction assumes the value is expressed
     * in FUnits and not pixels.
     * <p/>
     * Since the CVT has been scaled to pixel values, the value taken from the
     * stack is scaled to the appropriate pixel value before being written to the
     * table.
     */
    public static final int WCVTF = 0x70;


    /**
     * WCVTP[] Write Control Value Table in Pixel units
     * <p/>
     * Code Range	 0x44<br />
     * Pops	 v: value in pixels (F26Dot6) <br />
     * l: control value table location (uint32) <br />
     * Pushes	 - <br />
     * Sets	 control value table entry <br />
     * Related instructions	 WCVTF[ ] <br />
     * <p/>
     * Writes the value in pixels into the control value table location specified.
     * <p/>
     * Pops a value v and a control value table location l from the stack and
     * puts that value in the specified location in the control value table. This
     * instruction assumes the value taken from the stack is in pixels and not in
     * FUnits. The value is written to the CVT table unchanged. The location l must
     * be less than the number of storage locations specified in the 'maxp' table
     * in the font file.
     */
    public static final int WCVTP = 0x44;

    /**
     * WS[] Write Store
     * <p/>
     * Code Range	 0x42 <br />
     * Pops	 v: storage area value (uint32) <br />
     * l: storage area location (uint32)<br />
     * Pushes	 -<br />
     * Sets	 storage area value  <br />
     * Related instructions	 RS[ ] <br />
     * <p/>
     * Write the value taken from the stack to the specified storage area location.
     * <p/>
     * Pops a storage area location l, followed by a value, v. Writes this 32-bit
     * value into the storage area location indexed by l. The value must be less
     * than the number of storage locations specified in the 'maxp' table of the
     * font file.
     */
    public static final int WS = 0x42;
}
