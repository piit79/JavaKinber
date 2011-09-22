package javakinber;

import java.net.*;

/**
 *
 * @author error
 */
public class KinberContact implements Comparable {

    private InetAddress address;
    private String nickName;
    private long updated;
    private long called;

    public KinberContact() {
        super();
    }

    public KinberContact(String aNickName, InetAddress anAddr) {
        super();
        address = anAddr;
        nickName = aNickName;
        setUpdated();
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String aNickName) {
        nickName = aNickName;
        setUpdated();
    }

    public InetAddress getAddress() {
        return address;
    }

    public String getIP() {
        return KinberUtil.getIP(getAddress());
    }

    public void setAddress(InetAddress addr) {
        address = addr;
    }

    public synchronized long getUpdated() {
        return updated;
    }

    public synchronized void setUpdated() {
        updated = System.currentTimeMillis();
        called = 0;
    }

    public synchronized long getUpdatedTime() {
        return System.currentTimeMillis() - updated;
    }

    public synchronized long getCalled() {
        return called;
    }

    public synchronized void setCalled() {
        called = System.currentTimeMillis();
    }

    public synchronized long getCalledTime() {
        return System.currentTimeMillis() - called;
    }

    public boolean wasCalled() {
        return called > 0;
    }

    public int compareTo(Object b) {
        return toString().compareTo(b.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if((obj == null) || (obj.getClass() != this.getClass()))
            return false;
        KinberContact c = (KinberContact)obj;
        return address.equals(c.getAddress()) && nickName.equals(c.getNickName());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + (this.address != null ? this.address.hashCode() : 0);
        hash = 89 * hash + (this.nickName != null ? this.nickName.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return nickName;
    }

    public String toStringV() {
        return nickName + " (" + getIP() + ")";
    }

}
