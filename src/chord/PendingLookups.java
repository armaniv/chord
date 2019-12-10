package chord;

import java.util.ArrayList;
import java.util.HashMap;

public class PendingLookups {
	// ASSUMPTION: a node can serve only 1 request per key per time
	// if a node tries to add an already existent lookup, lookup will 
	// not be added to the pending lookups and it will be actually ignored
	private HashMap<Integer, Lookup> pendingLookups;
	
	public PendingLookups() {
		this.pendingLookups = new HashMap<>();
	}
	
	public Boolean addLookup(Lookup lookup) {
		if (!this.pendingLookups.containsKey(lookup.getKey())) {
			this.pendingLookups.put(lookup.getKey(), lookup);
			return Boolean.TRUE;
		}else {
			return Boolean.FALSE;
		}
	}
	
	// updates if already exists
	public void updateLookup(Lookup lookup) {
		this.pendingLookups.put(lookup.getKey(), lookup);
	}
	
	public Lookup getLookup(Integer key) {
		return this.pendingLookups.get(key);
	}
	
	public Lookup removeLookup(Integer key) {
		return this.pendingLookups.remove(key);
	}
	
	// if i was not able to over pass that node, it means that it failed
	// method to be called at least 5 THINKS after lookup has been forwarded
	// to the first successor
	public Boolean isPathBroken(Integer nodeId, Lookup lookup) {
		ArrayList<Integer> messagePath = lookup.getMessagePath();
		for (int i=0; i<messagePath.size(); i++) {
			if (messagePath.get(i).equals(nodeId)) {
				if (messagePath.size()<(i+1)+1) {
					return Boolean.TRUE;
				}else {
					return Boolean.FALSE;
				}
			}
		}
		return Boolean.FALSE;	// never reached
	}
	
	public Boolean containsLookupFor(Integer key) {
		return this.pendingLookups.containsKey(key);
	}
}
