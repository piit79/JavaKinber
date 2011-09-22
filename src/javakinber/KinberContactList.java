package javakinber;

import java.util.*;
import java.net.*;
import javax.swing.*;

/**
 *
 * @author error
 */
public class KinberContactList implements List<KinberContact>, Iterable<KinberContact> {

    public List<KinberContact> contactList;
    private KinberListModel listModel;
    private long lastRefresh;

    public KinberContactList() {
        super();
        contactList = new ArrayList<KinberContact>();
        listModel = new KinberListModel();
    }

    public synchronized int getIndexByAddress(InetAddress addr) {
        KinberContact c;
        for (int i = 0; i < contactList.size(); i++) {
            c = contactList.get(i);
            if (addr.equals(c.getAddress())) return i;
        }
        return -1;
    }

    public synchronized KinberContact get(InetAddress addr) {
        int index = getIndexByAddress(addr);
        if (index == -1) return null;
        else return get(index);
    }

    public synchronized boolean add(KinberContact contact) {
        return add(contact, true);
    }

    public synchronized boolean add(KinberContact contact, boolean update) {
        int index = getIndexByAddress(contact.getAddress());
        if (index > -1) {
            KinberContact curContact = contactList.get(index);
            if (contact.equals(curContact)) {
                curContact.setUpdated();
                return true;
            }
            else {
                curContact.setNickName(contact.getNickName());
                listModel.fireContentsChanged(index, index);
            }
        }
        else {
            contactList.add(contact);
            listModel.fireIntervalAdded(contactList.size()-1, contactList.size()-1);
        }
        if (update) {
            updateList();
        }
        return true;
    }

    public synchronized void add(int index, KinberContact contact) {
        contactList.add(index, contact);
        listModel.fireIntervalAdded(index, index);
    }

    public void updateList() {
        Collections.sort(contactList);
        listModel.fireContentsChanged(0, contactList.size()-1);
    }

    public ListModel getListModel() {
        return listModel;
    }

    public synchronized int size() {
        return contactList.size();
    }

    public synchronized boolean isEmpty() {
        return contactList.isEmpty();
    }

    public boolean contains(Object o) {
        return contactList.contains((KinberContact)o);
    }

    public Iterator iterator() {
        return contactList.iterator();
    }

    public KinberContact[] toArray() {
        return (KinberContact[])contactList.toArray();
    }

    public KinberContact[] toArray(Object[] a) {
        return (KinberContact[])contactList.toArray(a);
    }

    public synchronized boolean remove(Object o) {
        KinberContact contact = (KinberContact)o;
        int index = getIndexByAddress(contact.getAddress());
        if (index != -1) {
            boolean retval = contactList.remove(contact);
            listModel.fireIntervalRemoved(index, index);
            return retval;
        }
        return false;
    }

    public boolean containsAll(Collection c) {
        return contactList.containsAll(c);
    }

    public synchronized boolean addAll(Collection c) {
        return contactList.addAll(c);
    }

    public synchronized boolean addAll(int index, Collection c) {
        return contactList.addAll(index, c);
    }

    public synchronized boolean removeAll(Collection c) {
        return contactList.removeAll(c);
    }

    public synchronized boolean retainAll(Collection c) {
        return contactList.retainAll(c);
    }

    public synchronized void clear() {
        int lastIndex = size()-1;
        contactList.clear();
        if (lastIndex > -1)
            listModel.fireIntervalRemoved(0, lastIndex);
    }

    public synchronized KinberContact get(int index) {
        return contactList.get(index);
    }

    public synchronized KinberContact set(int index, KinberContact contact) {
        KinberContact c = contactList.set(index, contact);
        listModel.fireContentsChanged(index, index);
        return c;
    }

    public synchronized KinberContact remove(int index) {
        KinberContact c = (KinberContact)contactList.remove(index);
        listModel.fireIntervalRemoved(index, index);
        return c;
    }

    public int indexOf(Object o) {
        return contactList.indexOf(o);
    }

    public int lastIndexOf(Object o) {
        return contactList.lastIndexOf(o);
    }

    public ListIterator listIterator() {
        return contactList.listIterator();
    }

    public ListIterator listIterator(int index) {
        return contactList.listIterator(index);
    }

    public List subList(int fromIndex, int toIndex) {
        return contactList.subList(fromIndex, toIndex);
    }

    public synchronized long getRefreshTime() {
        return System.currentTimeMillis() - lastRefresh;
    }

    public synchronized void setRefreshed() {
        lastRefresh = System.currentTimeMillis();
    }

    protected class KinberListModel extends AbstractListModel implements ListModel {

        public KinberListModel() {
            super();
        }

        public Object getElementAt(int index) {
            //if (index > contactList.size()-1) return null;
            //else return contactList.get(index);
            return contactList.get(index);
        }

        public int getSize() {
            return contactList.size();
        }

        public void fireContentsChanged(int index0, int index1) {
            fireContentsChanged(this, index0, index1);
        }

        public void fireIntervalAdded(int index0, int index1) {
            fireIntervalAdded(this, index0, index1);
        }

        public void fireIntervalRemoved(int index0, int index1) {
            fireIntervalRemoved(this, index0, index1);
        }

    }

}
