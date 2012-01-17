package GemIdentTools;

import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

@SuppressWarnings("unchecked")
public class ValueSortedMap <K extends Comparable,V extends Comparable> {
	SortedSet<KV> kvSet;
	HashMap<K,KV> map;
	int reverse;
	public class KV implements Comparable<KV>{
		public K key;
		public V value;
		public int compareTo(KV o) {
			int val = value.compareTo(o.value) * reverse;
			if (val == 0)
				val = key.compareTo(o.key);
			return val;
			// TODO Auto-generated method stub
		}
	}
	public ValueSortedMap(){
		this(false);
	}
	public ValueSortedMap(boolean reverse){
		if (reverse)
			this.reverse = -1;
		else
			this.reverse = 1;
		kvSet = new TreeSet<KV>();
		map = new HashMap<K,KV>();
	}
	public void put(K key, V value){
		KV kv = new KV();
		kv.key = key;
		kv.value = value;
		map.put(key,kv);
		kvSet.add(kv);
	}
	public void remove(K key){
		KV kv = map.get(key);
		if (kv == null)
			return;
		kvSet.remove(kv);
		map.remove(key);		
	}
	public SortedSet<KV> keySet(){
		return kvSet;
	}
	public V get(K key){
		return map.get(key).value;
	}
	public K firstKey(){
		return kvSet.first().key;
	}
	public K lastKey(){
		return kvSet.last().key;
	}	
	public int size(){
		return kvSet.size();
	}	
	public Iterator<K> keyIterator(){
		return new KeyIterator();
	}
	
    private class KeyIterator implements Iterator {

    	private Iterator<KV> current;

		KeyIterator(){
    		current = kvSet.iterator();
    	}
    	
		public boolean hasNext() {
			return current.hasNext();
		}

		public K next() {
			return  current.next().key;
		}

		public void remove() {
			current.remove();
		};
    	
    }
	public void clear() {
		kvSet.clear();
		map.clear();
	}
}
