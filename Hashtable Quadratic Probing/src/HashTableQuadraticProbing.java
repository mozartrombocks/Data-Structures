import java.util.*;

@SuppressWarnings("unchecked")
public class HashTableQuadraticProbing <K, V> implements Iterable <K> {
	
	private double loadFactor;
	private int capacity, threshold, modificationCount = 0;
	
	private int usedBuckets = 0, keyCount = 0;
	
	private K [] keyTable;
	private V [] valueTable;
	
	private boolean containsFlag = false;
	
	private final K TOMBSTONE = (K) (new Object());
	
	private static final int DEFAULT_CAPACITY = 8;
	private static final double DEFAULT_LOAD_FACTOR = 0.45;
	
	public HashTableQuadraticProbing() {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}
	
	public HashTableQuadraticProbing(int capacity) {
		this(capacity, DEFAULT_LOAD_FACTOR);
	}
	
	public HashTableQuadraticProbing(int capacity, double loadFactor) {
		
		if (capacity <= 0)throw new IllegalArgumentException("Illegal capacity: " + capacity);
		
	    if (loadFactor <= 0 || Double.isNaN(loadFactor) || Double.isInfinite(loadFactor))
	    	throw new IllegalArgumentException("Illegal loadFactor: " + loadFactor);
		
		this.loadFactor = loadFactor;
		this.capacity = Math.max(DEFAULT_CAPACITY, next2Power(capacity));
		threshold = (int) (this.capacity + loadFactor);
		
		keyTable = (K[]) new Object[this.capacity];
		valueTable = (V[]) new Object[this.capacity];
	}
	
	private static int next2Power(int n) {
		return Integer.highestOneBit(n) << 1;
	}
	
	private static int P(int x) {
		return (x*x + x) >> 1;
	}
	
	private int normalizeIndex(int keyHash) {
		return (keyHash & 0x7FFFFFFF) % capacity;
	}
	
	public void clear() {
		for(int i=0; i<capacity; i++) {
			keyTable[i] = null;
			valueTable[i] = null;
			}
		keyCount = usedBuckets = 0;
		modificationCount++;
	}
	
	public int size() { return keyCount; }
       
	public boolean isEmpty() { return keyCount == 0; }
	
	public V put(K key, V value) { return insert(key, value); }
	
	public V add(K key, V value) { return insert(key, value); }
	
	public V insert(K key, V val) {
		
		if (key==null) throw new IllegalArgumentException("Null key");
		if (usedBuckets >= threshold) resizeTable();
		
		final int hash = normalizeIndex(key.hashCode());
		int i = hash, j = -1, x = 1;
		
		do {
			if(keyTable[i] == TOMBSTONE) {
				if(j == -1) j=i;
			} else if(keyTable[i] !=  null) {
				if (keyTable[i].equals(key)) {
					V oldValue = valueTable[i];
					if(j == -1) {
						valueTable[i] = val;
					} else {
						keyTable[i] = TOMBSTONE;
						valueTable[i] = null;
						keyTable[j] = key;
						valueTable[j] = val;
					}
					modificationCount++;
					return oldValue;
				}
			} else {
				if(j == -1) {
					usedBuckets++; keyCount++;
					keyTable[i] = key;
					valueTable[i] = val;
			} else {
				keyCount++;
				keyTable[j] = key;
				valueTable[j] = val;
			} 
				modificationCount++;
				return null;
			}
			    i = normalizeIndex(hash + P(x++));	
		} while(true);
	}
	
	public boolean containsKey(K key) {
		return hasKey(key);
	}
	
	public boolean hasKey(K key) {
		get(key);
		
		return containsFlag;
	}
	
	public V get(K key) {
		if(key == null) throw new IllegalArgumentException("Null key");
		final int hash = normalizeIndex(key.hashCode());
		int i = hash, j = -1, x = 1;
		
		do {
			
			if (keyTable[i] == TOMBSTONE) {
				if (j == -1) j = i;
				} else if (keyTable[i] != null) {
					if (keyTable[i].equals(key)) {
						containsFlag = true;
						
						if (j != -1) {
							keyTable[j] = keyTable[i];
							valueTable[j] = valueTable[i];
							
							keyTable[i] = TOMBSTONE;
							valueTable[i] = null;
							
							return valueTable[j];
							} else { 
								return valueTable[i];
							}
					}
				} else {
					containsFlag = false;
					return null;
				}
		   i = normalizeIndex(hash + P(x++));
	}  while ( true );
}
	
	public V remove(K key) {
		if (key == null) throw new IllegalArgumentException("Null key");
		
		final int hash = normalizeIndex(key.hashCode());
		int i = hash, x = 1;
		
		for(;; i = normalizeIndex(hash + P(x++)) ) {
			if(keyTable[i] == TOMBSTONE) continue;
			if(keyTable[i] == null) return null;
			if(keyTable[i].equals(key)) {
				keyCount--;
				modificationCount++;
				V oldValue = valueTable[i];
				keyTable[i] = TOMBSTONE;
				return oldValue;
			}
		}
	}
	
	public List <K> keys() {
		List <K> keys = new ArrayList<>(size());
		for(int i = 0; i < capacity; i++)
			if(keyTable[i] != null && keyTable[i] != TOMBSTONE)
				keys.add(keyTable[i]);
			    return keys;
	}
	
	public List <V> values() {
		List <V> values = new ArrayList<>(size());
		for(int i = 0; i < capacity; i++)
			if(keyTable[i] != null && keyTable[i] != TOMBSTONE)
				values.add(valueTable[i]);
		return values;
	}
	
	private void resizeTable() {
		capacity *= 2;
		threshold = (int) (capacity * loadFactor);
		
		K[] oldKeyTable = (K[]) new Object[capacity];
	    V[] oldValueTable = (V[]) new Object[capacity];
	    
	    K[] keyTableTmp = keyTable;
	    keyTable = oldKeyTable;
	    oldKeyTable = keyTableTmp;
	    
	    V[] valueTableTmp = valueTable;
	    valueTable = oldValueTable;
	    oldValueTable = valueTableTmp;
	    
	    keyCount = usedBuckets = 0;
	   
	    for(int i = 0; i < oldKeyTable.length; i++) {
	    	insert(oldKeyTable[i], oldValueTable[i]);
	    	oldValueTable[i] = null;
	    	oldKeyTable[i] = null;
	    }
		
	}
	
	@Override public java.util.Iterator <K> iterator() {
		final int MODIFICATION_COUNT = modificationCount;
		
		return new java.util.Iterator <K> () {
			int keysLeft = keyCount, index = 0;
			
			@Override public boolean hasNext() {
				if (MODIFICATION_COUNT != modificationCount) throw new java.util.ConcurrentModificationException();
				return keysLeft != 0;
			}
			
			@Override public K next() {
				while( keyTable[index] == null || keyTable[index] == TOMBSTONE) index++;
				keysLeft--;
				return keyTable[index++];
			}
			
			@Override public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
	@Override public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("{");
		for(int i = 0; i < capacity; i++)
			if(keyTable[i] != null && keyTable[i] != TOMBSTONE)
				sb.append(keyTable[i] + " => " + valueTable[i] + ", ");
		sb.append("}");
		return sb.toString();
		
	}
}

	
	


