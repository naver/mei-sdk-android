/**
 * Original source from https://github.com/bumptech/glide/blob/master/third_party/gif_encoder/src/main/java/com/bumptech/glide/gifencoder/LZWEncoder.java
 * Ported java to c language
 */
#include "LZWEncoder.h"
#include <stdlib.h>
#include <android/log.h>

#define EOF         -1
#define FALSE       0
#define TRUE        1
#define MAX_BITS    12
#define MAX_CODE    4096
#define MAX_BUFFER_SIZE 1048576
#define HASH_SHIFTER    3

#define MAX(a, b) ((a > b) ? a : b)

#define  BITS  12
#define HSIZE  5003 // 80% occupancy

static int imgW, imgH;
static jbyte *pixAry;
static int initCodeSize;
static int remaining;
static int curPixel;


static int n_bits; // number of bits/code

static int maxbits = BITS; // user settable max # bits/code

static int maxcode; // maximum code, given n_bits

static int maxmaxcode = 1 << BITS; // should NEVER generate this code

static int htab[HSIZE];

int codetab[HSIZE];

static int hsize = HSIZE; // for dynamic table sizing

static int free_ent = 0; // first unused entry

// block compression parameters -- after all codes are used up,
// and compression rate changes, start over.
static int clear_flg = FALSE;


static int g_init_bits;

static int ClearCode;

static int EOFCode;

static int cur_accum;

static int cur_bits;

static int masks[] = {0x0000, 0x0001, 0x0003, 0x0007, 0x000F, 0x001F, 0x003F, 0x007F, 0x00FF,
                      0x01FF,
                      0x03FF, 0x07FF, 0x0FFF, 0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF};

// Define the storage for the packet accumulator
static jbyte accum[256];

// Number of characters so far in this 'packet'
static int a_count;


// declare functions
inline void write(int byte, jbyte *byteArray, int *length);

void compress(int init_bits, register jbyte *buffer, register int *bufferLength);

jbyteArray toJbyteArrayAndClean(JNIEnv *env, jbyte *byteArr, int legnth);

// Flush the packet to disk, and reset the accumulator
void flush_char(jbyte *buffer, int *bufferLength);

// reset code table
void cl_hash(register int hsize);

void output(int code, jbyte *buffer, int *bufferLength);

int MAXCODE(int n_bits);

inline int nextPixel();

void writeRange(register jbyte *bytes, register int start, register int end, jbyte *outArray,
                register int *length);

// table clear for block compress
void cl_block(jbyte *buffer, int *bufferLength);

JNIEXPORT jbyteArray JNICALL Java_com_naver_mei_sdk_core_gif_encoder_NativeLZWEncoder_compress
		(JNIEnv *env, jclass jclazz, jbyteArray jpixels, jint width, jint height, jint colorDepth) {
	jbyte *pixels = (*env)->GetByteArrayElements(env, jpixels, NULL);

	jbyte *buffer = malloc(
			MAX_BUFFER_SIZE);    // android os version에 따른 stack 사이즈 이슈로 인해 heap 영역으로 이동
	int bufferLength = 0;
	imgW = width;
	imgH = height;
	pixAry = pixels;
	initCodeSize = MAX(2, colorDepth);
	remaining = imgW * imgH; // reset navigation variables
	curPixel = 0;
	write(initCodeSize, buffer, &bufferLength);

	compress(initCodeSize + 1, buffer, &bufferLength); // compress and write the pixel data

	write(0, buffer, &bufferLength); // write block terminator

	(*env)->ReleaseByteArrayElements(env, jpixels, pixels, 0);
	return toJbyteArrayAndClean(env, buffer, bufferLength);
}

void compress(int init_bits, register jbyte *buffer, register int *bufferLength) {
	register int fcode;
	register int i /* = 0 */;
	register int c;
	register int code;
	register int disp;
	register int hsize_reg;
	register int hshift;
	// Set up the globals: g_init_bits - initial number of bits
	g_init_bits = init_bits;

	// Set up the necessary values
	clear_flg = FALSE;
	n_bits = g_init_bits;
	cur_accum = 0;
	cur_bits = 0;
	maxcode = MAXCODE(n_bits);

	ClearCode = 1 << (init_bits - 1);
	EOFCode = ClearCode + 1;
	free_ent = ClearCode + 2;

	a_count = 0; // clear packet

	code = nextPixel();

	hshift = 0;
	for (fcode = hsize; fcode < 65536; fcode *= 2)
		++hshift;
	hshift = 8 - hshift; // set hash code range bound

	hsize_reg = hsize;
	cl_hash(hsize_reg); // clear hash table

	output(ClearCode, buffer, bufferLength);

	int isOuterContinue = FALSE;
	while ((c = nextPixel()) != EOF) {
		fcode = (c << maxbits) + code;
		i = (c << hshift) ^ code; // xor hashing

		if (htab[i] == fcode) { // is exists
			code = codetab[i];
			continue;
		} else if (htab[i] >= 0) // non-empty slot, collision
		{
			disp = hsize_reg - i; // secondary hash (after G. Knott)
			if (i == 0)
				disp = 1;
			do {
				if ((i -= disp) < 0)
					i += hsize_reg;

				if (htab[i] == fcode) {
					code = codetab[i];
					isOuterContinue = TRUE;
					break;
				}
			} while (htab[i] >= 0);

			if (isOuterContinue == TRUE) {
				isOuterContinue = FALSE;
				continue;
			}
		}

		output(code, buffer, bufferLength);
		code = c;
		if (free_ent < maxmaxcode) {
			codetab[i] = free_ent++; // code -> hashtable
			htab[i] = fcode;
		} else
			cl_block(buffer, bufferLength);
	}

	// Put out the final code.
	output(code, buffer, bufferLength);
	output(EOFCode, buffer, bufferLength);
}


// Add a character to the end of the current packet, and if it is 254
// characters, flush the packet to disk.
void char_out(jbyte c, jbyte *buffer, int *bufferLength) {
	accum[a_count++] = c;
	if (a_count >= 254)
		flush_char(buffer, bufferLength);
}

// Clear out the hash table

// table clear for block compress
void cl_block(jbyte *buffer, int *bufferLength) {
	cl_hash(hsize);
	free_ent = ClearCode + 2;
	clear_flg = TRUE;

	output(ClearCode, buffer, bufferLength);
}

// reset code table
void cl_hash(register int hsize) {
	register int i;
	for (i = 0; i < hsize; ++i)
		htab[i] = -1;
}

// Flush the packet to disk, and reset the accumulator
void flush_char(jbyte *buffer, int *bufferLength) {
	if (a_count > 0) {
		write(a_count, buffer, bufferLength);
		writeRange(accum, 0, a_count, buffer, bufferLength);
		a_count = 0;
	}
}

int MAXCODE(int n_bits) {
	return (1 << n_bits) - 1;
}


int nextPixel() {
	if (remaining == 0) return EOF;

	--remaining;
	return pixAry[curPixel++] & 0xff;
}

void output(int code, jbyte *buffer, int *bufferLength) {
	cur_accum &= masks[cur_bits];

	if (cur_bits > 0)
		cur_accum |= (code << cur_bits);
	else
		cur_accum = code;

	cur_bits += n_bits;

	while (cur_bits >= 8) {
		char_out((jbyte) (cur_accum & 0xff), buffer, bufferLength);
		cur_accum >>= 8;
		cur_bits -= 8;
	}

	// If the next entry is going to be too big for the code size,
	// then increase it, if possible.
	if (free_ent > maxcode || clear_flg) {
		if (clear_flg) {
			maxcode = MAXCODE(n_bits = g_init_bits);
			clear_flg = FALSE;
		} else {
			++n_bits;
			if (n_bits == maxbits)
				maxcode = maxmaxcode;
			else
				maxcode = MAXCODE(n_bits);
		}
	}

	if (code == EOFCode) {
		// At EOF, write the rest of the buffer.
		while (cur_bits > 0) {
			char_out((jbyte) (cur_accum & 0xff), buffer, bufferLength);
			cur_accum >>= 8;
			cur_bits -= 8;
		}

		flush_char(buffer, bufferLength);
	}
}

jbyteArray toJbyteArrayAndClean(JNIEnv *env, jbyte *byteArr, int length) {
	jbyteArray byteArrayForJava = (*env)->NewByteArray(env, length);
	(*env)->SetByteArrayRegion(env, byteArrayForJava, 0, length, byteArr);
	free(byteArr);
	return byteArrayForJava;
}

void write(int byte, jbyte *outArray, int *length) {
	outArray[(*length)++] = (jbyte) (byte);
}

void writeRange(jbyte *inArray, register int start, register int end, jbyte *outArray,
                int *length) {
	register int i;
	for (i = start; i < end; ++i) {
        outArray[(*length)++] = inArray[i];
	}
}