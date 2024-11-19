package land.src.toolbox.jvm.oop.gc.g1

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.oop.gc.shared.CollectedHeap
import land.src.toolbox.jvm.primitive.Address

class G1CollectedHeap(address: Address) : CollectedHeap(address) {

    val heapRegionManager: HeapRegionManager by nonNull("_hrm", isPointer = false)
    val archiveSet: HeapRegionSetBase by nonNull("_archive_set", isPointer = false)
    var summaryBytesUsed: Long by nonNull("_summary_bytes_used")

    var currentNode: HeapRegion? = null

    override fun allocate(size: Long): Long {
        // grab a block from the free list and make it available for allocation
        if (currentNode == null || !currentNode!!.fits(size)) {
            currentNode = heapRegionManager.freeList.removeFromHead()

            // we set the thing to be a closed archive region
            val closedArchiveType: Int = 32 or 8 + 1
            currentNode!!.regionType = closedArchiveType

            // increase the archive set length
            archiveSet.length++

            // TODO: this will only work on ASSERT builds?
            currentNode!!.containingSet = archiveSet
        }

        val node = currentNode!!
        val result = node.top
        node.top += size

        summaryBytesUsed += size

        return result
    }

}