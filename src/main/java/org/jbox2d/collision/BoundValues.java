package org.jbox2d.collision;

import java.io.Serializable;

public class BoundValues implements Serializable {
	// Matt: these need to stay public regardless of what intellij says.
	public final int[] lowerValues = new int[2];
	public final int[] upperValues = new int[2];
}
