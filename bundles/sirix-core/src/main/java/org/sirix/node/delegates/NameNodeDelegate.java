/**
 * Copyright (c) 2011, University of Konstanz, Distributed Systems Group
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the University of Konstanz nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/**
 * 
 */
package org.sirix.node.delegates;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nonnull;

import org.sirix.api.visitor.EVisitResult;
import org.sirix.api.visitor.IVisitor;
import org.sirix.node.ENode;
import org.sirix.node.interfaces.INameNode;
import org.sirix.node.interfaces.INode;

/**
 * Delegate method for all nodes containing \"naming\"-data. That means that
 * different fixed defined names are represented by the nodes delegating the
 * calls of the interface {@link INameNode} to this class. Mainly, keys are
 * stored referencing later on to the string stored in dedicated pages.
 * 
 * @author Sebastian Graf, University of Konstanz
 * 
 */
public class NameNodeDelegate implements INameNode {

  /** Node delegate, containing basic node information. */
  private final NodeDelegate mDelegate;
  /** Key of the name. The name contains the prefix as well. */
  private int mNameKey;
  /** URI of the related namespace. */
  private int mUriKey;

  /**
   * Constructor.
   * 
   * @param pDel
   *          page delegator
   * @param pNameKey
   *          namekey to be stored
   * @param pUriKey
   *          urikey to be stored
   */
  public NameNodeDelegate(@Nonnull final NodeDelegate pDel, final int pNameKey, final int pUriKey) {
    mDelegate = checkNotNull(pDel);
    mNameKey = pNameKey;
    mUriKey = pUriKey;
  }

  /**
   * Delegate method for setHash.
   * 
   * @param pHash
   * @see org.sirix.node.delegates.NodeDelegate#setHash(long)
   */
  @Override
  public void setHash(final long pHash) {
    mDelegate.setHash(pHash);
  }

  /**
   * Delegate method for getHash.
   * 
   * @return
   * @see org.sirix.node.delegates.NodeDelegate#getHash()
   */
  @Override
  public long getHash() {
    return mDelegate.getHash();
  }

  /**
   * Delegate method for setKey.
   * 
   * @param pKey
   *            key to set
   * @see org.sirix.node.delegates.NodeDelegate#setNodeKey(long)
   */
  @Override
  public void setNodeKey(final long pKey) {
    mDelegate.setNodeKey(pKey);
  }

  /**
   * Delegate method for getKey.
   * 
   * @return
   * @see org.sirix.node.delegates.NodeDelegate#getNodeKey()
   */
  @Override
  public long getNodeKey() {
    return mDelegate.getNodeKey();
  }

  /**
   * Delegate method for getParentKey.
   * 
   * @return
   * @see org.sirix.node.delegates.NodeDelegate#getParentKey()
   */
  @Override
  public long getParentKey() {
    return mDelegate.getParentKey();
  }

  /**
   * Delegate method for hasParent.
   * 
   * @return
   * @see org.sirix.node.delegates.NodeDelegate#hasParent()
   */
  @Override
  public boolean hasParent() {
    return mDelegate.hasParent();
  }

  /**
   * Delegate method for getTypeKey.
   * 
   * @return
   * @see org.sirix.node.delegates.NodeDelegate#getTypeKey()
   */
  @Override
  public int getTypeKey() {
    return mDelegate.getTypeKey();
  }

  /**
   * Delegate method for setParentKey.
   * 
   * @param pNodeKey
   * @see org.sirix.node.delegates.NodeDelegate#setParentKey(long)
   */
  @Override
  public void setParentKey(final long pNodeKey) {
    mDelegate.setParentKey(pNodeKey);
  }

  /**
   * Delegate method for setType.
   * 
   * @param pTypeKey
   * @see org.sirix.node.delegates.NodeDelegate#setTypeKey(int)
   */
  @Override
  public void setTypeKey(final int pTypeKey) {
    mDelegate.setTypeKey(pTypeKey);
  }

  @Override
  public ENode getKind() {
    return ENode.NAMESPACE_KIND;
  }

  @Override
  public EVisitResult acceptVisitor(final IVisitor pVisitor) {
    return mDelegate.acceptVisitor(pVisitor);
  }

  @Override
  public int getNameKey() {
    return mNameKey;
  }

  @Override
  public int getURIKey() {
    return mUriKey;
  }

  @Override
  public void setNameKey(final int pNameKey) {
    mNameKey = pNameKey;
  }

  @Override
  public void setURIKey(final int pUriKey) {
    mUriKey = pUriKey;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + mNameKey;
    result = prime * result + mUriKey;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    NameNodeDelegate other = (NameNodeDelegate)obj;
    if (mNameKey != other.mNameKey)
      return false;
    if (mUriKey != other.mUriKey)
      return false;
    return true;
  }

  /**
   * Delegate method for toString.
   * 
   * @return
   * @see org.sirix.node.delegates.NodeDelegate#toString()
   */
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append("\nuri key: ");
    builder.append(mUriKey);
    builder.append("\nname key: ");
    builder.append(mNameKey);
    return builder.toString();
  }
  
  @Override
  public boolean isSameItem(final INode pOther) {
    return mDelegate.isSameItem(pOther);
  }
}