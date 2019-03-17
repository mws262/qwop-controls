/*
 * JBox2D - A Java Port of Erin Catto's Box2D
 *
 * JBox2D homepage: http://jbox2d.sourceforge.net/
 * Box2D homepage: http://www.box2d.org
 *
 * This software is provided 'as-is', without any express or implied
 * warranty.  In no event will the authors be held liable for any damages
 * arising from the use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 * 1. The origin of this software must not be misrepresented; you must not
 * claim that you wrote the original software. If you use this software
 * in a product, an acknowledgment in the product documentation would be
 * appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 * misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package org.jbox2d.common;

/**
 * A few math methods that don't fit very well anywhere else.
 */
public class MathUtils {
	public static final float PI = (float) Math.PI;
	public static final float TWOPI = (float) (Math.PI * 2);


	public static final float[] sinLUT = new float[Settings.SINCOS_LUT_LENGTH];
	public static final float[] cosLUT = new float[Settings.SINCOS_LUT_LENGTH];

	static {
		for(int i=0; i<Settings.SINCOS_LUT_LENGTH; i++){
			sinLUT[i] = (float) Math.sin( i * Settings.SINCOS_LUT_PRECISION);
			cosLUT[i] = (float) Math.cos( i * Settings.SINCOS_LUT_PRECISION);
		}
	}

	public static float sin(float x){
		if(Settings.SINCOS_LUT_ENABLED){
			x %= TWOPI;

			while(x < 0){
				x += TWOPI;
			}

			if(Settings.SINCOS_LUT_LERP){

				x /= Settings.SINCOS_LUT_PRECISION;

				final int index = (int)x;

				if(index != 0){
					x %= index;
				}

				// the next index is 0
				if(index == Settings.SINCOS_LUT_LENGTH - 1){
					return ( (1-x)*sinLUT[index] + x * sinLUT[0]);
				}else{
					return ( (1-x)*sinLUT[index] + x * sinLUT[index + 1]);
				}

			}else{
				return sinLUT[ MathUtils.round(x / Settings.SINCOS_LUT_PRECISION) % Settings.SINCOS_LUT_LENGTH];
			}

		}else{
			return (float) Math.sin(x);
		}
	}

	public static float cos(float x){
		if(Settings.SINCOS_LUT_ENABLED){
			x %= TWOPI;

			while(x < 0){
				x += TWOPI;
			}

			if(Settings.SINCOS_LUT_LERP){

				x /= Settings.SINCOS_LUT_PRECISION;

				final int index = (int)x;

				if(index != 0){
					x %= index;
				}

				// the next index is 0
				if(index == Settings.SINCOS_LUT_LENGTH-1){
					return ( (1-x)*cosLUT[index] + x * cosLUT[0]);
				}else{
					return ( (1-x)*cosLUT[index] + x * cosLUT[index + 1]);
				}

			}else{
				return cosLUT[ MathUtils.round(x / Settings.SINCOS_LUT_PRECISION) % Settings.SINCOS_LUT_LENGTH];
			}

		}else{
			return (float) Math.cos(x);
		}
	}

	public static float abs(final float x) {
		if (Settings.FAST_MATH) {
			return x > 0 ? x : -x;
		}
		else {
			return Math.abs(x);
		}
	}

	public static int floor(final float x) {
		if (Settings.FAST_MATH) {
			return x > 0 ? (int) x : (int) x - 1;
		}
		else {
			return (int) Math.floor(x);
		}
	}

	public static int ceil(final float x){
		if (Settings.FAST_MATH){
			return floor(x+.5f);
		}else{
			return (int) Math.ceil(x);
		}
	}

	public static int round(final float x){
		if(Settings.FAST_MATH){
			return floor(x + .5f);
		}else{
			return Math.round(x);
		}
	}

	// Max/min rewritten here because for some reason MathUtils.max/min
	// can run absurdly slow for such simple functions...
	// TODO: profile, see if this just seems to be the case or is actually
	// causing issues...
	public static float max(final float a, final float b) {
		return a > b ? a : b;
	}

	public static int max(final int a, final int b) {
		return a > b ? a : b;
	}

	public static float min(final float a, final float b) {
		return a < b ? a : b;
	}

	public static float map(final float val, final float fromMin, final float fromMax,
							final float toMin, final float toMax) {
		final float mult = (val - fromMin) / (fromMax - fromMin);
		return toMin + mult * (toMax - toMin);
	}

	/** Returns the closest value to 'a' that is in between 'low' and 'high' */
	public final static float clamp(final float a, final float low, final float high) {
		return max(low, min(a, high));
	}

	/* djm optimized */
	public final static Vec2 clamp(final Vec2 a, final Vec2 low, final Vec2 high) {
		final Vec2 min = new Vec2();
		Vec2.minToOut(a, high, min);
		Vec2.maxToOut(low, min, min);
		return min;
	}

	/* djm created */
	public final static void clampToOut(final Vec2 a, final Vec2 low, final Vec2 high,
										final Vec2 dest) {
		Vec2.minToOut(a, high, dest);
		Vec2.maxToOut(low, dest, dest);
	}

	public final static boolean isPowerOfTwo(final int x) {
		return x > 0 && (x & x - 1) == 0;
	}


	/**
	 * Computes a fast approximation to <code>Math.pow(a, b)</code>.
	 * Adapted from <url>http://www.dctsystems.co.uk/Software/power.html</url>.
	 *
	 * @param a a positive number
	 * @param b a number
	 * @return a^b
	 */
	// UNTESTED
	public static float pow(final float a, float b) {
		// adapted from: http://www.dctsystems.co.uk/Software/power.html
		if (Settings.FAST_MATH) {
			float x = Float.floatToRawIntBits(a);
			x *= 1.0f / (1 << 23);
			x = x - 127;
			float y = x - MathUtils.floor(x);
			b *= x + (y - y * y) * 0.346607f;
			y = b - MathUtils.floor(b);
			y = (y - y * y) * 0.33971f;
			return Float.intBitsToFloat((int) ((b + 127 - y) * (1 << 23)));
		}
		else {
			return (float) Math.pow(a, b);
		}
	}

	public static float sqrt(float x) {
		return (float)StrictMath.sqrt(x);
	}
}