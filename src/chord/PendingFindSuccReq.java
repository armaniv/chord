package chord;

import java.util.ArrayList;
import java.util.HashMap;

public class PendingFindSuccReq {
	// ASSUMPTION: a node can serve only 1 request per key per time
	// if a node tries to add an already existent lookup, lookup will 
	// not be added to the pending lookups and it will be actually ignored
	private HashMap<Integer, FindSuccReq> pendingRequests;
	
	public PendingFindSuccReq() {
		this.pendingRequests = new HashMap<>();
	}
	
	public Boolean addRequest(FindSuccReq req) {
		if (!this.pendingRequests.containsKey(req.getId())) {
			this.pendingRequests.put(req.getId(), req);
			return Boolean.TRUE;
		}else {
			return Boolean.FALSE;
		}
	}
	
	// updates if already exists
	public void updateRequest(FindSuccReq req) {
		this.pendingRequests.put(req.getId(), req);
	}
	
	public FindSuccReq getRequest(Integer id) {
		return this.pendingRequests.get(id);
	}
	
	public FindSuccReq removeRequest(Integer id) {
		return this.pendingRequests.remove(id);
	}
	
	// if i was not able to over pass that node, it means that it failed
	// method to be called at least 5 THINKS after lookup has been forwarded
	// to the first successor
	public Boolean isPathBroken(Integer nodeId, Integer id) {
		if (!this.pendingRequests.containsKey(id)) {
			return Boolean.FALSE;
		}
		FindSuccReq request = this.pendingRequests.get(id);
		ArrayList<Integer> messagePath = request.getMessagePath();
		for (int i=0; i<messagePath.size(); i++) {
			if (messagePath.get(i).equals(nodeId)) {
				if (messagePath.size()<(i+1)+1) {
					return Boolean.TRUE;
				}else {
					return Boolean.FALSE;
				}
			}
		}
		return Boolean.TRUE;	// never reached
	}
}
