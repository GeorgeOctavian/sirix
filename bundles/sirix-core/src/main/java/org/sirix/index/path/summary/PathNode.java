package org.sirix.index.path.summary;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.util.path.Path;
import org.sirix.node.NodeKind;
import org.sirix.node.delegates.NameNodeDelegate;
import org.sirix.node.delegates.NodeDelegate;
import org.sirix.node.delegates.StructNodeDelegate;
import org.sirix.node.interfaces.NameNode;
import org.sirix.node.xml.AbstractStructForwardingNode;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Path node in the {@link PathSummaryReader}.
 *
 * @author Johannes Lichtenberger
 */
public final class PathNode extends AbstractStructForwardingNode implements NameNode {

  /**
   * {@link NodeDelegate} instance.
   */
  private final NodeDelegate mNodeDel;

  /**
   * {@link StructNodeDelegate} instance.
   */
  private final StructNodeDelegate mStructNodeDel;

  /**
   * {@link NameNodeDelegate} instance.
   */
  private final NameNodeDelegate mNameNodeDel;

  /**
   * Kind of node to index.
   */
  private final NodeKind mKind;

  /**
   * The node name.
   */
  private final QNm mName;

  /**
   * Number of references to this path node.
   */
  private int mReferences;

  /**
   * Level of this path node.
   */
  private int mLevel;

  /**
   * Constructor.
   *
   * @param name          the full qualified name
   * @param nodeDel       {@link NodeDelegate} instance
   * @param structNodeDel {@link StructNodeDelegate} instance
   * @param nameNodeDel   {@link NameNodeDelegate} instance
   * @param kind          kind of node to index
   * @param references    number of references to this path node
   * @param level         level of this path node
   */
  public PathNode(final QNm name, final NodeDelegate nodeDel, @Nonnull final StructNodeDelegate structNodeDel,
      @Nonnull final NameNodeDelegate nameNodeDel, @Nonnull final NodeKind kind, @Nonnegative final int references,
      @Nonnegative final int level) {
    mName = checkNotNull(name);
    mNodeDel = checkNotNull(nodeDel);
    mStructNodeDel = checkNotNull(structNodeDel);
    mNameNodeDel = checkNotNull(nameNodeDel);
    mKind = checkNotNull(kind);
    checkArgument(references > 0, "references must be > 0!");
    mReferences = references;
    mLevel = level;
  }

  /**
   * Get the path up to the root path node.
   *
   * @param reader {@link PathSummaryReader} instance
   * @return path up to the root
   */
  public Path<QNm> getPath(final PathSummaryReader reader) {
    PathNode node = this;
    final long nodeKey = reader.getNodeKey();
    reader.moveTo(node.getNodeKey());
    final PathNode[] pathNodes = new PathNode[mLevel];
    for (int i = mLevel - 1; i >= 0; i--) {
      pathNodes[i] = node;
      node = reader.moveToParent().trx().getPathNode();
    }

    final Path<QNm> path = new Path<>();
    for (final PathNode pathNode : pathNodes) {
      reader.moveTo(pathNode.getNodeKey());
      if (pathNode.getPathKind() == NodeKind.ATTRIBUTE) {
        path.attribute(reader.getName());
      } else {
        final QNm name;
        if (reader.getPathKind() == NodeKind.OBJECT_KEY) {
          name = new QNm(null, null, reader.getName().getLocalName().replace("/", "\\/"));
          path.child(name);
        } else if (reader.getPathKind() == NodeKind.ARRAY) {
          path.childArray();
        } else {
          name = reader.getName();
          path.child(name);
        }
      }
    }
    reader.moveTo(nodeKey);
    return path;
  }

  /**
   * Level of this path node.
   *
   * @return level of this path node
   */
  public int getLevel() {
    return mLevel;
  }

  /**
   * Get the number of references to this path node.
   *
   * @return number of references
   */
  public int getReferences() {
    return mReferences;
  }

  /**
   * Set the reference count.
   *
   * @param references number of references
   */
  public void setReferenceCount(final @Nonnegative int references) {
    checkArgument(references > 0, "pReferences must be > 0!");
    mReferences = references;
  }

  /**
   * Increment the reference count.
   */
  public void incrementReferenceCount() {
    mReferences++;
  }

  /**
   * Decrement the reference count.
   */
  public void decrementReferenceCount() {
    if (mReferences <= 1) {
      throw new IllegalStateException();
    }
    mReferences--;
  }

  /**
   * Get the kind of path (element, attribute or namespace).
   *
   * @return path kind
   */
  public NodeKind getPathKind() {
    return mKind;
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.PATH;
  }

  @Override
  public int getPrefixKey() {
    return mNameNodeDel.getPrefixKey();
  }

  @Override
  public int getLocalNameKey() {
    return mNameNodeDel.getLocalNameKey();
  }

  @Override
  public int getURIKey() {
    return mNameNodeDel.getURIKey();
  }

  @Override
  public void setLocalNameKey(final int nameKey) {
    mNameNodeDel.setLocalNameKey(nameKey);
  }

  @Override
  public void setPrefixKey(final int prefixKey) {
    mNameNodeDel.setPrefixKey(prefixKey);
  }

  @Override
  public void setURIKey(final int uriKey) {
    mNameNodeDel.setURIKey(uriKey);
  }

  @Override
  protected StructNodeDelegate structDelegate() {
    return mStructNodeDel;
  }

  @Override
  protected NodeDelegate delegate() {
    return mNodeDel;
  }

  /**
   * Get the name node delegate.
   *
   * @return name node delegate.
   */
  public NameNodeDelegate getNameNodeDelegate() {
    return mNameNodeDel;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(mNodeDel, mNameNodeDel);
  }

  @Override
  public boolean equals(final @Nullable Object obj) {
    if (obj instanceof PathNode) {
      final PathNode other = (PathNode) obj;
      return Objects.equal(mNodeDel, other.mNodeDel) && Objects.equal(mNameNodeDel, other.mNameNodeDel);
    }
    return false;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
                      .add("node delegate", mNodeDel)
                      .add("struct delegate", mStructNodeDel)
                      .add("name delegate", mNameNodeDel)
                      .add("references", mReferences)
                      .add("kind", mKind)
                      .add("level", mLevel)
                      .toString();
  }

  @Override
  public void setPathNodeKey(final long pNodeKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getPathNodeKey() {
    throw new UnsupportedOperationException();
  }

  @Override
  public QNm getName() {
    return mName;
  }

}
