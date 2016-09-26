package ftp;

/**
 * This class is used in the "hash" method in FTP.java to generate a complicated
 * hash result given a byte array. The additional class was made to further
 * the algorithm's complexity to avoid collisions when authenticating users when
 * logging in.
 */
public class Hasher {
    
    /**
     * This method uses the S-Boxes to generate a double byte array of
     * seemingly random values. The byte arrays in each index, and each index of 
     * those byte arrays, are shuffled in various ways. The byte arrays are then
     * XORed to generate a single byte array of length 4, and that byte array
     * is returned.
     * @param input
     * @return 
     */
    public static byte[] hash(byte[] input) {
        if(input.length<1) 
            input = new byte[]{0};
        
        byte[][] sboxConversion = sboxConversion(input);
        for(int r = 0; r<sboxConversion.length; r++) 
            sboxConversion[r] = cycleBytes(sboxConversion[r], r);
        
        for(int r = 0; r<sboxConversion.length; r++) 
            for(int c = 0; c<4; c++) {
                byte b = sboxConversion[r][c];
                for(int i=0; i<(r+c); i++) {
                    b = SBoxes.convSbox(b, (r*c));
                    sboxConversion[r][c] = b;
                }
            }
        
        byte[] hash = xorMatrix(sboxConversion);
        return hash;
    }
    
    /**
     * XORs multiple byte arrays together to generate and return one byte array.
     * @param matrix
     * @return 
     */
    private static byte[] xorMatrix(byte[][] matrix) {
        if(matrix.length < 1) 
            return null;
        if(matrix.length == 1) 
            return matrix[0];
        
        byte[] xor = matrix[0];
        for(int i=1; i<matrix.length; i++) {
            xor = xorByteArrays(xor,matrix[i]);
        }
        
        return xor;
    }
    
    /**
     * This method shuffles bytes in a byte array around, shifting each byte
     * "number" times.
     * @param bytes
     * @param number
     * @return 
     */
    private static byte[] cycleBytes(byte[] bytes, int number) {
        byte[] result = new byte[bytes.length];
        
        for(int i=0; i< bytes.length; i++) 
            result[(i+number) % bytes.length] = bytes[i];
        
        return result;
    }
    
    /**
     * This method uses the S-Boxes to generate a double byte array from a
     * regular byte array. For each element in the byte array, it gets four
     * results from the four S-Boxes, to generate a "bytes.length" long byte 
     * array, and each element has a length of four.
     * @param bytes
     * @return 
     */
    private static byte[][] sboxConversion(byte[] bytes) {
        byte[][] result = new byte[bytes.length][4];
        for(int i=0; i<bytes.length; i++)
            result[i] = SBoxes.sboxByte(bytes[i]);
        return result;
    }
    
    /**
     * XORs two byte arrays together and returns the result.
     * @param a
     * @param b
     * @return 
     */
    private static byte[] xorByteArrays(byte[] a, byte[] b) {
        if(a==null) 
            return null;
        if(b==null) 
            return null;
        
        byte[] result = new byte[a.length];
        for(int i=0; i<a.length; i++)
            result[i] = (byte)(a[i] ^ b[i]);
        
        return result;
    }
    
    /**
     * This class contains the S-Boxes, along with various methods. The S-Boxes
     * are used to return seemingly random results given a byte that represents
     * an index of the S-Boxes.
     */
    static class SBoxes {
    
        private static final byte[] SBOX1 = {-22,44,-86,-106,-36,-50,-102,-1,60,16,-112,-48,21,-52,-44,-32,82,69,62,-38,33,70,-111,-105,54,43,1,17,-117,66,-6,-69,91,-9,27,-89,-74,-104,124,-92,-70,-42,-114,97,-67,123,79,7,63,-17,101,-10,104,46,-5,45,55,-47,126,22,32,-66,73,96,-113,119,121,-60,56,-95,-64,65,59,86,-58,90,-99,115,37,-107,-128,92,3,98,-27,-51,-39,-18,108,-109,-125,-78,15,9,99,2,120,-103,-108,8,100,-87,19,80,41,-35,-24,-84,89,-126,-127,47,-23,-75,68,48,-72,103,-79,51,-116,87,-29,57,127,-110,53,34,110,12,5,125,61,-59,74,-57,35,-93,-4,-41,58,-7,-100,31,-56,-33,95,23,93,106,-118,88,109,-123,38,-97,-61,-19,6,102,114,-11,0,-63,-20,-46,113,94,14,-121,-94,-21,-26,111,-77,4,-43,42,-13,-65,52,24,-120,-85,-124,-54,81,-83,116,-12,-40,78,-71,-88,-101,-15,26,-119,-8,-91,25,76,122,-82,-76,30,77,-115,-16,-96,20,83,-14,-28,-98,-45,85,18,-73,-81,-53,-49,-31,-34,105,107,50,-62,-80,-37,67,-68,-55,13,-3,84,72,118,10,112,64,-122,29,40,39,49,-25,-30,11,117,-90,28,71,75,-2,36};
        private static final byte[] SBOX2 = {100,-119,103,29,83,25,84,38,-43,-34,-26,-36,27,-56,-73,-3,-45,-29,-25,20,-2,14,59,-105,89,111,110,-72,-110,-61,50,-125,92,-120,44,30,-115,-87,-50,-35,-98,22,-19,63,-63,99,-39,-96,-70,17,10,127,13,-71,-4,-76,-15,-8,-24,51,-51,-27,33,-92,120,-83,-37,106,77,4,61,-100,-112,-10,75,123,49,31,-53,15,-107,9,-6,-94,34,18,74,124,-28,-44,82,60,109,46,-13,58,122,-128,-58,3,26,115,-111,-1,-108,-106,114,79,-41,80,45,-122,-55,32,-80,40,-46,-79,-47,12,-74,78,-118,68,-104,16,8,126,-5,6,42,-99,47,64,-109,-54,48,-82,116,73,88,-93,24,-65,56,102,39,-60,94,105,70,97,-101,-7,2,-57,41,112,23,121,57,-62,11,36,-31,86,-116,-16,-20,65,-67,-18,-69,-84,5,-88,95,125,-90,-89,-66,90,72,-42,19,-91,87,43,35,52,-12,-126,107,-30,108,21,-85,-49,93,-40,-124,91,-52,-86,-97,98,1,-95,81,-121,-38,-114,-117,-11,-48,-17,-123,-64,-113,101,-68,62,-21,53,85,-33,66,-59,-23,54,-78,-102,119,117,-32,96,113,0,104,55,-77,28,71,37,-9,118,-22,-103,-127,69,-14,-75,-81,7,67,76};
        private static final byte[] SBOX3 = {33,-8,-119,70,62,104,43,42,-32,-71,-21,51,57,86,121,-31,27,63,-3,-122,-64,-41,69,119,-24,-45,109,100,73,-37,-27,14,-57,54,-125,-107,96,-128,-12,115,6,-25,-23,5,-82,30,-9,-4,-98,58,1,-10,56,-16,-43,20,-84,-94,23,18,48,61,83,-62,-108,10,-80,-99,-54,-87,75,24,49,-104,64,-97,-95,94,108,-34,-102,95,-42,76,-126,-51,-40,25,-5,-53,-2,-65,-28,-68,-127,15,107,-61,80,-20,66,87,-15,122,-112,-83,46,125,2,92,-90,-72,-100,-63,40,-86,-120,-111,-109,-49,78,-48,116,89,-88,21,106,41,-117,85,-123,-58,-59,39,32,88,3,52,55,31,-7,-36,-70,-56,19,9,-47,59,-113,-121,60,71,-46,-91,90,37,-14,-33,-124,-74,117,112,16,-1,22,65,103,101,-19,93,111,0,120,-29,29,-55,98,-93,28,47,74,84,113,11,81,-106,-44,50,12,102,67,7,38,114,-50,77,35,118,68,-77,-11,53,-18,-115,123,44,-85,-26,-118,-52,34,-75,-116,79,-81,-35,26,-66,-67,127,-96,45,124,91,-110,-30,-92,4,72,-79,-22,36,-17,-105,17,-114,105,-89,97,13,126,-76,-38,-39,-78,99,110,8,-60,-13,-6,82,-69,-73,-103,-101};
        private static final byte[] SBOX4 = {-120,-81,33,49,-101,50,-84,-68,37,-99,-62,-29,97,-70,-112,-75,-115,-47,-118,107,-17,-80,-7,-114,58,-3,84,-23,-96,0,-107,-14,-25,-27,-69,-116,26,-117,-119,86,-57,-4,-122,-15,43,-58,-90,-59,77,-67,-42,-52,113,8,93,34,55,27,-60,83,54,-88,-38,53,68,-55,-56,24,119,-13,-46,-33,59,6,-50,48,21,-1,17,-72,-26,-63,-97,-20,-65,-48,57,95,-127,-77,117,-73,31,-34,76,108,69,19,32,39,-108,13,98,-113,-30,-109,-124,90,38,41,103,-22,102,-16,29,106,-76,-121,67,7,3,36,-92,85,72,114,-93,-51,-12,16,-104,80,23,123,-32,-64,110,66,74,-95,-83,-11,99,-18,104,-39,-21,-66,63,79,35,-2,4,-89,120,100,-79,1,-105,45,56,101,-125,-102,51,47,-110,18,-128,-91,-35,-9,78,-37,-40,5,-103,-6,60,-78,94,-44,-126,118,82,70,-45,115,-10,111,96,87,-74,-28,-61,61,125,65,30,-86,-94,-85,-49,109,-98,88,112,-5,-31,-36,-54,-87,124,122,11,71,10,-100,92,14,-71,20,-106,116,12,-19,64,-43,22,91,-123,52,105,81,-53,25,28,62,-82,40,-111,9,-24,15,75,-8,-41,46,42,127,89,121,73,2,44,126};
        
        /**
         * Returns the appropriate S-Box given a number between 0 and 3.
         * @param i
         * @return 
         */
        public static byte[] getSboxFromNum(int i)
        {
            switch(i%4) {
                case 0:
                    return SBOX1;
                case 1:
                    return SBOX2;
                case 2:
                    return SBOX3;
                case 3:
                    return SBOX4;
            } 
            
            return SBOX1;
        }
        
        /**
         * Returns a byte array of length 4 that represents an index of each
         * S-Box; the index is given by byte b, which is converted to an
         * unsigned byte and then integer.
         * @param b
         * @return 
         */
        public static byte[] sboxByte(byte b) {
            return new byte[] { convSbox1(b), convSbox2(b), convSbox3(b), convSbox4(b) };
        }
        
        /**
         * Returns an integer interpretation of an unsigned byte given a signed 
         * byte (default in Java).
         * @param b
         * @return 
         */
        private static int unsignedToBytes(byte b) {
            return b & 0xFF;
        }
        
        /**
         * Returns the S-Box value at a given index (from byte b). int i
         * represents a specific S-Box between 1 and 4.
         * @param b
         * @param i
         * @return 
         */
        public static byte convSbox(byte b, int i) {
            int k = unsignedToBytes(b);
            byte[] sbox = getSboxFromNum(i);
            return sbox[k];
        }

         /**
         * Returns the S-Box1 value at a given index (from byte b).
         * @param b
         * @param i
         * @return 
         */
        public static byte convSbox1(byte b) {
            int k = unsignedToBytes(b);
            return SBOX1[k];
        }
        
        /**
         * Returns the S-Box2 value at a given index (from byte b).
         * @param b
         * @param i
         * @return 
         */
        private static byte convSbox2(byte b) {
            int k = unsignedToBytes(b);
            return SBOX2[k];
        }

        /**
         * Returns the S-Box3 value at a given index (from byte b).
         * @param b
         * @param i
         * @return 
         */
        private static byte convSbox3(byte b) {
            int k = unsignedToBytes(b);
            return SBOX3[k];
        }

        /**
         * Returns the S-Box4 value at a given index (from byte b).
         * @param b
         * @param i
         * @return 
         */
        private static byte convSbox4(byte b) {
            int k = unsignedToBytes(b);
            return SBOX4[k];
        }
    }
}
