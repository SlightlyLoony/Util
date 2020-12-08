package com.dilatush.util.noisefilter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * <p>Implements a filter that removes noise from a series of measurement values, where "noise" is defined as readings that are not plausible for
 * the data being filtered.  The filter works by constructing chains of plausibly related samples, then throwing away any samples that aren't part of
 * the longest such chain.  This method works for data series where any given sample <i>should</i> have a value that's relatively close to the
 * preceding samples' values, such as a series of temperature or humidity values.</p>
 * <p>The "plausibility" of the value of a given sample is determined by measuring the "distance" of the new sample to all the existing samples
 * contained in the filter.  The existing sample with the smallest distance value is chosen as the new sample's ancestor, thus determining which chain
 * it belongs to.  Distance is computed by the distance function passed into the constructor.  This function <i>must</i> be implemented to fit the
 * specific characteristics of the data stream being filtered.  See {@link Distance} for further discussion on this.</p>
 * <p>Note that the {@link #toString()} method returns a string containing a representation of the entire sample tree.  This is very useful when
 * debugging and tuning {@link Distance} function.</p>
 */
public class NoiseFilter {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern( "mm:ss.SSS" ).withZone( ZoneId.systemDefault() );

    private final long     depthMS;   // the depth of this filter's sample tree, in milliseconds...
    private final Distance distance;  // the distance function used by this filter...

    private SampleTreeNode root = null;  // the root of this filter's sample tree...


    /**
     * <p>Creates a new instance of this class with no samples and the given parameters.</p>
     * <p>The {@link #depthMS} is how many milliseconds of samples will be retained in the filter.  The more samples that are retained, the better
     * the discrimination between the chain lengths - which translates into better rejection of longer noise bursts.  The fewer samples retained,
     * the less computation is required for all filter operations.  Life is full of tradeoffs!</p>
     * <p>The {@link #distance} function is critical to the operation of this filter.  See {@link Distance} for more details.</p>
     *
     * @param _depthMS the number of milliseconds of samples that will be retained in the filter
     * @param _distance the distance function for this filter
     */
    public NoiseFilter( final long _depthMS, final Distance _distance ) {
        distance = _distance;
        depthMS     = _depthMS;
    }


    /**
     * Adds the given new sample to the filter.  The new sample's distance from each existing sample in the filter is computed, and the new sample
     * is linked to the existing sample that it is "closest" to.
     *
     * @param _sample the sample to add
     */
    public void addSample( final Sample _sample ) {

        // create our shiny new node...
        SampleTreeNode newNode = new SampleTreeNode( _sample );

        // if this is the first sample added, it just becomes the root...
        if( root == null ) {
            root = newNode;
            return;
        }

        // otherwise, traverse the existing sample tree to find closest existing sample, and then link to it...
        NodeDistance closestNode = findClosestNode( root, newNode );
        linkNewNode( newNode, closestNode.node );

        // increment the branch size all the way down to the root...
        SampleTreeNode currentNode = newNode;
        while( currentNode.parent != null ) {
            currentNode.parent.branchSize++;
            currentNode = currentNode.parent.thisNode;
        }

        // we might have changed the order of things, so resort...
        sortFwdLinks( root );
    }


    /**
     * Prune any elements in the sample tree that older than the configured depth (see {@link #NoiseFilter(long, Distance)}).  Note that the root is
     * always the oldest element, so the method employed here is to simply prune the root repeatedly until the root doesn't need pruning.
     *
     * @param _now prune relative to this time
     */
    public void prune( final Instant _now ) {

        // sort our links...
        sortFwdLinks( root );

        // while we still have a root that's at least as old as our pruning threshold...
        Instant pruneTo = _now.minusMillis( depthMS );
        while( (root != null) && root.sample.timestamp.isBefore( pruneTo ) ) {

            // if there are no children of the root, null the root and leave...
            if( root.fwdLinks.size() == 0 ) {
                root = null;
                continue;
            }

            // set our new root...
            root = root.fwdLinks.get(0).fwdNode;
        }
    }


    /**
     * <p>Returns the filtered sample, if the sample tree is at least as deep as the given depth parameter; otherwise, returns
     * <code>null</code>.  The given depth must be less than the tree's depth by at least one nominal sampling interval.</p>
     * <p>The sample returned is the one on the longest (most plausible) chain of samples that is closest to, but earlier than,
     * the given time minus the given noise margin.</p>
     *
     * @param _minDepth the minimum depth of the tree, in milliseconds, before a reading will be returned
     * @param _noiseMargin the noise margin in milliseconds
     * @param _now the time the noise margin is relative to
     * @return the measurement reading found, or <code>null</code> if none was found
     */
    public Sample sampleAt( final long _minDepth, final long _noiseMargin, final Instant _now ) {

        // sort our links...
        sortFwdLinks( root );

        // if we have no tree yet, return a null...
        if( root == null )
            return null;

        // if our tree isn't deep enough yet, return a null...
        if( _minDepth > (_now.toEpochMilli() - root.sample.timestamp.toEpochMilli()) )
            return null;

        // we have a tree, so follow the longest branch until we find either the end of the branch, or a node with a later timestamp...
        Instant measurementTime = _now.minusMillis( _noiseMargin );
        SampleTreeNode current = root;
        while( (current.fwdLinks.size() > 0) && (current.fwdLinks.get(0).fwdNode.sample.timestamp.isBefore( measurementTime )) ) {
            current = current.fwdLinks.get(0).fwdNode;
        }

        // when we get here, the current node's reading is the one we want, so return it...
        return current.sample;
    }


    /**
     * Returns a string representation of the sample tree contained in this instance.
     *
     * @return the string representation of this instance
     */
    public String toString() {

        // sort our links...
        sortFwdLinks( root );

        // traverse the sample tree to find the root node of all the branches...
        List<SampleTreeNode> branchRoots = new ArrayList<>();
        branchRoots.add( root );   // add our root in, 'cause findRoots won't get it...
        findRoots( root, branchRoots );
        branchRoots.sort( ( _stn1, _stn2 ) -> (int) (_stn2.sample.timestamp.toEpochMilli() - _stn1.sample.timestamp.toEpochMilli()) );

        // get our branches into a list (ordered from oldest to youngest root) of lists of branch members (ordered from oldest to youngest)...
        List<BranchDescriptor> branches = new ArrayList<>();
        for( SampleTreeNode branchRoot : branchRoots ) {
            branches.add( new BranchDescriptor( branchRoot, branches.size() ) );
        }

        // get our line descriptors, in temporal order...
        List<LineDescriptor> lines = new ArrayList<>();
        int remainingBranches = branches.size();
        while( remainingBranches > 0 ) {

            // find the oldest reading...
            BranchDescriptor oldestBranch = null;
            for( BranchDescriptor branch : branches ) {
                if( branch.index < branch.members.size() ) {
                    if( (oldestBranch == null) || branch.getIndexedTimestamp().isBefore( oldestBranch.getIndexedTimestamp() ) ) {
                        oldestBranch = branch;
                    }
                }
            }
            assert oldestBranch != null;

            // add a line descriptor for this bad boy...
            lines.add( new LineDescriptor( oldestBranch ) );

            // bump the index on our oldest find...
            oldestBranch.index++;

            // if this was the last member, bump down the number of branches remaining to be exhausted...
            if( oldestBranch.index >= oldestBranch.members.size() )
                remainingBranches--;
        }

        // we're finally ready to start building our result string...
        StringBuilder result = new StringBuilder();
        for( LineDescriptor line : lines ) {

            // first the timestamp -- just minutes, seconds, and milliseconds...
            result.append( TIMESTAMP_FORMATTER.format( line.node.sample.timestamp ) );

            // now we get all our columns...
            for( BranchDescriptor branch : branches ) {

                // are we in our reading's column?
                if( line.branch == branch.branchIndex ) {

                    // output the reading value as xxx.xx, 7 characters...
                    result.append( String.format( " %1$6.2f", line.node.sample.value ) );
                }

                // are we in this column's range?
                else if( branch.inRange( line.node.sample.timestamp ) ) {
                    result.append( "   |   " );
                }
                else {
                    result.append( "       " );
                }
            }

            // we'll need a line separator...
            result.append( System.lineSeparator() );
        }

        return result.toString();
    }


    /**
     * Simple tuple describing a branch, for use in {@link #toString()}.
     */
    private static class BranchDescriptor {
        private final List<SampleTreeNode> members;
        private final Instant              start;
        private final Instant              end;
        private       int                  index;
        private final int                  branchIndex;


        /**
         * Creates a new instance of this class.
         *
         * @param _branchRoot the root node of this branch
         * @param _branchIndex the index (with the branches list) of this branch
         */
        private BranchDescriptor( final SampleTreeNode _branchRoot, final int _branchIndex ) {
            members = new ArrayList<>();
            findMembers( _branchRoot, members );
            start = members.get( 0 ).sample.timestamp;
            end   = members.get( members.size() - 1 ).sample.timestamp;
            branchIndex = _branchIndex;
            index = 0;
        }


        /**
         * Returns true if the given time is between the start and end times of this branch.
         *
         * @param _time the time to test
         * @return true if the given time is between the start and end times of this branch
         */
        private boolean inRange( final Instant _time ) {
            return !( _time.isBefore( start ) || _time.isAfter( end ) );
        }


        /**
         * Returns the timestamp of the branch member at the current index within this branch.
         *
         * @return the timestamp of the branch member at the current index within this branch
         */
        private Instant getIndexedTimestamp() {
            return members.get( index ).sample.timestamp;
        }
    }


    /**
     * Simple tuple that describes one line of the output of {@link #toString()}.
     */
    private static class LineDescriptor {
        private final SampleTreeNode node;
        private final int branch;


        /**
         * Creates a new instance of this class for the given branch descriptor.
         *
         * @param _branchDescriptor the branch descriptor for this line
         */
        private LineDescriptor( final BranchDescriptor _branchDescriptor ) {
            node = _branchDescriptor.members.get( _branchDescriptor.index );
            branch = _branchDescriptor.branchIndex;
        }
    }


    /**
     * Recursively finds the members for the branch starting at the given node, adding them to the given list of members.
     *
     * @param _current the current node we're finding members for
     * @param _members the list of members found
     */
    private static void findMembers( final SampleTreeNode _current, final List<SampleTreeNode> _members ) {
        _members.add( _current );
        if( _current.fwdLinks.size() > 0 )
            findMembers( _current.fwdLinks.get(0).fwdNode, _members );
    }


    /**
     * Recursively finds the roots for all the branches of the existing sample tree, starting at the given node.
     *
     * @param _node the node to search for branches in
     * @param _roots the list of root nodes
     */
    private static void findRoots( final SampleTreeNode _node, final List<SampleTreeNode> _roots ) {

        // iterate over all our children...
        int childCount = 0;
        for( FwdLink fwdLink : _node.fwdLinks ) {

            // add our child's branch roots...
            findRoots( fwdLink.fwdNode, _roots );

            // if this is our first child (which is branch zero), then add it as a new branch root...
            childCount++;
            if( childCount > 1 ) {
                _roots.add( fwdLink.fwdNode );
            }
        }
    }


    /**
     * Recursively sorts all the forward links in the given node and its children, into order from the largest branch size to the smallest.
     *
     * @param _node the node to sort
     */
    private void sortFwdLinks( final SampleTreeNode _node ) {
        if( _node.fwdLinks.size() > 1 ) {
            Collections.sort( _node.fwdLinks );
        }
        for( FwdLink link : _node.fwdLinks ) {
            sortFwdLinks( link.fwdNode );
        }
    }


    /**
     * Links the given new node (for a sample being added to the sample tree) to the given closest node (the node representing the existing sample
     * that is most plausibly related to the new sample).
     *
     * @param _newNode     the node for the sample being added
     * @param _closestNode the node for the closest existing sample
     */
    private void linkNewNode( final SampleTreeNode _newNode, final SampleTreeNode _closestNode ) {

        // make a forward link in our parent...
        FwdLink newFwdLink = new FwdLink( _closestNode, _newNode );
        _closestNode.fwdLinks.add( newFwdLink );

        // set the parent link in our new child...
        _newNode.parent = newFwdLink;
    }


    /**
     * Recursively traverses the entire sample tree to find the existing sample that is closest to the given new node.
     *
     * @param _current the node currently being examined
     * @param _newNode the new node being compared with existing nodes
     * @return the tuple of the closest node and the closeness value
     */
    private NodeDistance findClosestNode( final SampleTreeNode _current, final SampleTreeNode _newNode ) {

        // compute the closeness of the new node to the current node...
        float dist = distance.calculate( _newNode.sample, _current.sample );
        NodeDistance closestNode = new NodeDistance( _current, dist );

        // now see if any of the current node's children are any closer...
        for( FwdLink childLink : _current.fwdLinks ) {

            closestNode = closest( closestNode, findClosestNode( childLink.fwdNode, _newNode ) );
        }

        // return the closest thing we found...
        return closestNode;
    }


    /**
     * Returns the closer of the two given close nodes.
     *
     * @param _a a node with its distance
     * @param _b another node with its distance
     * @return the closer of the two given nodes
     */
    private NodeDistance closest( final NodeDistance _a, final NodeDistance _b ) {
        return (_a.distance < _b.distance) ? _a : _b;
    }


    /**
     * A simple tuple to hold a node and its distance.
     */
    private static class NodeDistance {
        private final SampleTreeNode node;
        private final float distance;


        /**
         * Creates a new instance of this class for the given node and distance.
         *
         * @param _node the node to use
         * @param _distance the distance to use
         */
        public NodeDistance( final SampleTreeNode _node, final float _distance ) {
            node = _node;
            distance = _distance;
        }
    }


    /**
     * A simple tuple representing a single node in the sample tree.
     */
    private static class SampleTreeNode implements Comparable<SampleTreeNode> {
        private final Sample sample;
        private final List<FwdLink> fwdLinks;
        private FwdLink parent;


        /**
         * Creates a new instance of this class with the given sample.
         *
         * @param _sample the sample belonging to this node
         */
        public SampleTreeNode( final Sample _sample ) {
            sample = _sample;
            fwdLinks = new ArrayList<>();
        }


        /**
         * Returns a string representation of this node.
         *
         * @return a string representation of this node
         */
        public String toString() {
            return sample.toString();
        }


        /**
         * See {@link Comparable} for details.
         *
         * @param _o the object being compared
         * @return a negative, zero, or positive integer as the given object is less than, equal to, or greater than the given object
         */
        @Override
        public int compareTo( final SampleTreeNode _o ) {
            return (int) ((sample.timestamp.toEpochMilli() - _o.sample.timestamp.toEpochMilli()) >> 32);
        }
    }


    /**
     * A tuple representing a forward link from one sample tree node to a child node.
     */
    private static class FwdLink implements Comparable<FwdLink> {
        private final SampleTreeNode thisNode;
        private final SampleTreeNode fwdNode;
        private int branchSize;


        public FwdLink( final SampleTreeNode _thisNode, final SampleTreeNode _fwdNode ) {
            thisNode = _thisNode;
            fwdNode = _fwdNode;
            branchSize = 0;
        }


        /**
         * Compare sample nodes in descending order of branch size.
         *
         * @param _fwdLink the sample forward link to compare this instance to
         * @return per the interface's contract, in inverse order of forward link branch size
         */
        @Override
        public int compareTo( final FwdLink _fwdLink ) {
            return _fwdLink.branchSize - branchSize;
        }
    }

}
