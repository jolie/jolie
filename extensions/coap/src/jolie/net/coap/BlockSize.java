package jolie.net.coap;

public enum BlockSize {
    UNBOUND(-1, 65536),
    SIZE_16(0, 16),
    SIZE_32(1, 32),
    SIZE_64(2, 64),
    SIZE_128(3, 128),
    SIZE_256(4, 256),
    SIZE_512(5, 512),
    SIZE_1024(6, 1024);

    private int encodedSize;
    private int decodedSize;

    public static final int UNDEFINED = -1;
    private static final int SZX_MIN = 0;
    private static final int SZX_MAX = 6;

    BlockSize(int encodedSize, int decodedSize) {
	this.encodedSize = encodedSize;
	this.decodedSize = decodedSize;
    }

    public int getSzx() {
	return this.encodedSize;
    }

    public int getSize() {
	return this.decodedSize;
    }

    public static int getSize(long szx) {
	return getBlockSize(szx).getSize();
    }

    public static boolean isValid(long szx) {
	return !(szx < SZX_MIN) && !(szx > SZX_MAX);
    }

    public static long min(long szx1, long szx2)
	    throws IllegalArgumentException {
	if (szx1 < UNDEFINED || szx1 > SZX_MAX || szx2 < UNDEFINED
		|| szx2 > SZX_MAX) {
	    throw new IllegalArgumentException("SZX value out "
		    + "of allowed range.");
	} else if (szx1 == BlockSize.UNDEFINED) {
	    return szx2;
	} else if (szx2 == BlockSize.UNDEFINED) {
	    return szx1;
	} else {
	    return Math.min(szx1, szx2);
	}
    }

    public static BlockSize getBlockSize(long szx)
	    throws IllegalArgumentException {
	if (szx == BlockSize.UNDEFINED) {
	    return BlockSize.UNBOUND;
	} else if (szx == 0) {
	    return SIZE_16;
	} else if (szx == 1) {
	    return SIZE_32;
	} else if (szx == 2) {
	    return SIZE_64;
	} else if (szx == 3) {
	    return SIZE_128;
	} else if (szx == 4) {
	    return SIZE_256;
	} else if (szx == 5) {
	    return SIZE_512;
	} else if (szx == 6) {
	    return SIZE_1024;
	} else {
	    throw new IllegalArgumentException("Unsupported SZX value "
		    + "(Block Option): " + szx);
	}
    }
}
