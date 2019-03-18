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

package org.jbox2d.collision;

import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Settings;

import java.io.Serializable;

//Updated to rev 56->108->136 of b2PairManager.cpp/.h

public class PairManager implements Serializable {

	private static final int NULL_PAIR = Integer.MAX_VALUE;
	public static final int NULL_PROXY = Integer.MAX_VALUE;
	private static final int TABLE_CAPACITY = Settings.maxPairs;
	private static final int TABLE_MASK = PairManager.TABLE_CAPACITY - 1;

    private final Pair[] m_pairs;
	private int m_pairCount;
    private int[] m_hashTable;
	private BroadPhase m_broadPhase;
	private PairCallback m_callback;
	private int m_freePair;
	private final BufferedPair[] m_pairBuffer;
	private int m_pairBufferCount;

	public PairManager() {
		m_pairs = new Pair[Settings.maxPairs];
		m_hashTable = new int[PairManager.TABLE_CAPACITY];
		//m_next = new int[Settings.maxPairs];
		m_pairBuffer = new BufferedPair[Settings.maxPairs];

		assert MathUtils.isPowerOfTwo(PairManager.TABLE_CAPACITY);
		assert PairManager.TABLE_CAPACITY >= Settings.maxPairs;

		for (int i = 0; i < PairManager.TABLE_CAPACITY; ++i) {
			m_hashTable[i] = PairManager.NULL_PAIR;
		}
		m_freePair = 0;
		for (int i = 0; i < Settings.maxPairs; ++i) {
			m_pairs[i] = new Pair();
			m_pairs[i].proxyId1 = PairManager.NULL_PROXY;
			m_pairs[i].proxyId2 = PairManager.NULL_PROXY;
			m_pairs[i].userData = null;
			m_pairs[i].status = 0;
			m_pairs[i].next = i+1;
			m_pairBuffer[i] = new BufferedPair();
		}
		m_pairs[Settings.maxPairs-1].next = PairManager.NULL_PAIR;
		m_pairCount = 0;
		m_pairBufferCount = 0;
	}

	public void initialize(final BroadPhase broadPhase, final PairCallback callback) {
		m_broadPhase = broadPhase;
		m_callback = callback;
	}

	// Add a pair and return the new pair. If the pair already exists,
	// no new pair is created and the old one is returned.
	public Pair addPair(int proxyId1, int proxyId2) {
		if (proxyId1 > proxyId2) {
			// integer primitive swap
			proxyId1 += proxyId2;
			proxyId2 = proxyId1 - proxyId2;
			proxyId1 -= proxyId2;
		}

		final int hash = hash(proxyId1, proxyId2) & PairManager.TABLE_MASK;

		Pair pair = find(proxyId1, proxyId2, hash);
		if (pair != null) {
			return pair;
		}

		assert(m_pairCount < Settings.maxPairs):"Too many pairs ("+m_pairCount+" shape AABB overlaps) - this usually means you have too many bodies, or you need to increase Settings.maxPairs.";
		assert(m_freePair != PairManager.NULL_PAIR);

		final int pairIndex = m_freePair;
		pair = m_pairs[pairIndex];
		m_freePair = pair.next;

		pair.proxyId1 = proxyId1;
		pair.proxyId2 = proxyId2;
		pair.status = 0;
		pair.userData = null;
		pair.next = m_hashTable[hash];

		m_hashTable[hash] = pairIndex;
		++m_pairCount;

		return pair;
	}

	// Remove a pair, return the pair's userData.
	public Object removePair(int proxyId1, int proxyId2) {
		assert(m_pairCount > 0);

		if (proxyId1 > proxyId2) {
			// integer primitive swap (safe for small ints)
			proxyId1 += proxyId2;
			proxyId2 = proxyId1 - proxyId2;
			proxyId1 -= proxyId2;
		}

		final int hash = hash(proxyId1, proxyId2) & PairManager.TABLE_MASK;
		int derefnode = m_hashTable[hash];
		boolean isHash = true;
		int pderefnode = 0;
		while (derefnode != PairManager.NULL_PAIR) {
			if (equals(m_pairs[derefnode], proxyId1, proxyId2)) {
				if (isHash) {
					m_hashTable[hash] = m_pairs[m_hashTable[hash]].next;
				} else {
					m_pairs[pderefnode].next = m_pairs[derefnode].next;
				}

				final Pair pair = m_pairs[derefnode];
				final Object userData = pair.userData;

				// Scrub
				pair.next = m_freePair;
				pair.proxyId1 = PairManager.NULL_PROXY;
				pair.proxyId2 = PairManager.NULL_PROXY;
				pair.userData = null;
				pair.status = 0;

				m_freePair = derefnode;
				--m_pairCount;

				return userData;
			} else {
				pderefnode = derefnode;
				derefnode = m_pairs[derefnode].next;
				isHash = false;
			}
		}

		assert(false) : "Attempted to remove a pair that does not exist";
		return null;
	}

	/*
     * As proxies are created and moved, many pairs are created and destroyed. Even worse, the same
     * pair may be added and removed multiple times in a single time step of the physics engine. To reduce
     * traffic in the pair manager, we try to avoid destroying pairs in the pair manager until the
     * end of the physics step. This is done by buffering all the RemovePair requests. AddPair
     * requests are processed immediately because we need the hash table entry for quick lookup.
     * 
     * All user user callbacks are delayed until the buffered pairs are confirmed in Commit.
     * This is very important because the user callbacks may be very expensive and client logic
     * may be harmed if pairs are added and removed within the same time step.
	 */
	/**
	 * Buffer a pair for addition.
     * We may add a pair that is not in the pair manager or pair buffer.
     * We may add a pair that is already in the pair manager and pair buffer.
     * If the added pair is not a new pair, then it must be in the pair buffer (because RemovePair was called).
	 */
    void addBufferedPair(final int id1, final int id2) {
		assert(id1 != PairManager.NULL_PROXY && id2 != PairManager.NULL_PROXY);
		assert(m_pairBufferCount < Settings.maxPairs);

		final Pair pair = addPair(id1, id2);

		// If this pair is not in the pair buffer ...
		if (!pair.isBuffered()) {
			// This must be a newly added pair.
			assert(!pair.isFinal());

			// Add it to the pair buffer.
			pair.setBuffered();
			m_pairBuffer[m_pairBufferCount].proxyId1 = pair.proxyId1;
			m_pairBuffer[m_pairBufferCount].proxyId2 = pair.proxyId2;
			++m_pairBufferCount;

			assert(m_pairBufferCount <= m_pairCount);
		}

		// Confirm this pair for the subsequent call to Commit.
		pair.clearRemoved();
		if (BroadPhase.s_validate){
			validateBuffer();
		}
	}

	/**
	 * Buffer a pair for removal.
	 * @param id1
	 * @param id2
	 */
    void removeBufferedPair(final int id1, final int id2) {
		assert(id1 != PairManager.NULL_PROXY && id2 != PairManager.NULL_PROXY);
		assert(m_pairBufferCount < Settings.maxPairs);
		final Pair pair = find(id1, id2);

		if (pair == null) {
			// The pair never existed. This is legal (due to collision filtering).
			return;
		}

		// If this pair is not in the pair buffer ...
		if (!pair.isBuffered()) {
			// This must be an old pair.
			assert(pair.isFinal());

			pair.setBuffered();
			m_pairBuffer[m_pairBufferCount].proxyId1 = pair.proxyId1;
			m_pairBuffer[m_pairBufferCount].proxyId2 = pair.proxyId2;
			++m_pairBufferCount;

			assert(m_pairBufferCount <= m_pairCount);
		}

		pair.setRemoved();

		if (BroadPhase.s_validate) {
			validateBuffer();
		}
	}

	/**
	 * commits the proxies
	 */
    void commit() {
		int removeCount = 0;
		final Proxy[] proxies = m_broadPhase.m_proxyPool;

		for (int i = 0; i < m_pairBufferCount; ++i) {
			final Pair pair = find(m_pairBuffer[i].proxyId1, m_pairBuffer[i].proxyId2);
			assert(pair.isBuffered());
			pair.clearBuffered();

			assert(pair.proxyId1 < Settings.maxProxies && pair.proxyId2 < Settings.maxProxies);

			final Proxy proxy1 = proxies[pair.proxyId1];
			final Proxy proxy2 = proxies[pair.proxyId2];

			assert(proxy1.isValid());
			assert(proxy2.isValid());

			if (pair.isRemoved()) {
				// It is possible a pair was added then removed before a commit. Therefore,
				// we should be careful not to tell the user the pair was removed when the
				// the user didn't receive a matching add.
				if (pair.isFinal()) {
					m_callback.pairRemoved(proxy1.userData, proxy2.userData, pair.userData);
				}

				// Store the ids so we can actually remove the pair below.
				m_pairBuffer[removeCount].proxyId1 = pair.proxyId1;
				m_pairBuffer[removeCount].proxyId2 = pair.proxyId2;
				++removeCount;
			} else {
				assert(m_broadPhase.testOverlap(proxy1, proxy2));

				if (!pair.isFinal()) {
					pair.userData = m_callback.pairAdded(proxy1.userData, proxy2.userData);
					pair.setFinal();
				}
			}
		}

		for (int i = 0; i < removeCount; ++i) {
			removePair(m_pairBuffer[i].proxyId1, m_pairBuffer[i].proxyId2);
		}

		m_pairBufferCount = 0;
		if (BroadPhase.s_validate) {
			validateTable();
		}
	}

	/**
	 * Unimplemented - for debugging purposes only in C++ version
	 */
    private void validateBuffer() {}

	/**
	 * For debugging
	 */
    private void validateTable() {
		for (int i = 0; i < PairManager.TABLE_CAPACITY; ++i) {
			int index = m_hashTable[i];
			while (index != PairManager.NULL_PAIR) {
				final Pair pair = m_pairs[index];
				assert(!pair.isBuffered());
				assert(pair.isFinal());
				assert(!pair.isRemoved());
				assert(pair.proxyId1 != pair.proxyId2);
				assert(pair.proxyId1 < Settings.maxProxies);
				assert(pair.proxyId2 < Settings.maxProxies);

				final Proxy proxy1 = m_broadPhase.m_proxyPool[pair.proxyId1];
				final Proxy proxy2 = m_broadPhase.m_proxyPool[pair.proxyId2];

				assert(proxy1.isValid());
				assert(proxy2.isValid());
				assert(m_broadPhase.testOverlap(proxy1, proxy2));

				index = pair.next;
			}
		}
	}

	/**
	 * finds the pair with the given hash
	 * @param proxyId1
	 * @param proxyId2
	 * @param hash
	 * @return
	 */
	public Pair find(final int proxyId1, final int proxyId2, final int hash) {
		int index = m_hashTable[hash];
		while (index != PairManager.NULL_PAIR
				&& !equals(m_pairs[index], proxyId1, proxyId2)) {
			index = m_pairs[index].next;
		}

		if (index == PairManager.NULL_PAIR) {
			return null;
		}

		assert index < Settings.maxPairs;
		return m_pairs[index];
	}

	/**
	 * finds the pair, creates hash and uses {@link #find(int, int, int)}
	 * @param proxyId1
	 * @param proxyId2
	 * @return
	 */
	public Pair find(int proxyId1, int proxyId2) {
		if (proxyId1 > proxyId2) {
			final int tmp = proxyId1;
			proxyId1 = proxyId2;
			proxyId2 = tmp;
		}
		final int hash = hash(proxyId1, proxyId2) & PairManager.TABLE_MASK;
		return find(proxyId1, proxyId2, hash);
	}

	private int hash(final int proxyId1, final int proxyId2) {
		// djm: this operation here is pretty self explanitory, so i don't think I need to describe what's happening,
		// or what the result is
		int key = (proxyId2 << 16) | proxyId1;
		key = ~key + (key << 15);
		key = key ^ (key >>> 12);
		key = key + (key << 2);
		key = key ^ (key >>> 4);
		key = key * 2057;
		key = key ^ (key >>> 16);
		return key;
	}

	/**
	 * returns if the pair has the two proxy id's
	 * @param pair
	 * @param proxyId1
	 * @param proxyId2
	 * @return
	 */
	public final boolean equals(final Pair pair, final int proxyId1, final int proxyId2) {
		return pair.proxyId1 == proxyId1 && pair.proxyId2 == proxyId2;
	}

	/**
	 * returns if the pairs have the same proxy id's
	 * @param pair1
	 * @param pair2
	 * @return
	 */
	public final boolean equals(final BufferedPair pair1, final BufferedPair pair2) {
		return pair1.proxyId1 == pair2.proxyId1 && pair1.proxyId2 == pair2.proxyId2;
	}
}
