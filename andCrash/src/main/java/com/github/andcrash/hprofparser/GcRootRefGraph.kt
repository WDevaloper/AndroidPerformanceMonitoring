package com.github.andcrash.hprofparser

import java.util.*
import kotlin.collections.HashMap

sealed class RefField {

    data object NoField : RefField()

    data class MemberField(val field: com.github.andcrash.hprofparser.MemberField) : RefField()

    data class StaticField(val field: com.github.andcrash.hprofparser.StaticField) : RefField()

    data class ArrayElementField(
        val field: RefField,
        val index: Int
    ) : RefField()
}

sealed class RefTreeNode {

    abstract val dominatorField: RefField

    data class GCRoot(
        override val dominatorField: RefField = RefField.NoField,
        val refTree: RefTree?
    ) : RefTreeNode()

    data class Primitive(
        override val dominatorField: RefField,
        val value: Any
    ) : RefTreeNode()

    data class PrimitiveArray(
        override val dominatorField: RefField,
        val array: Any
    ) : RefTreeNode()

    data class ObjectArrayElement(
        override val dominatorField: RefField.ArrayElementField,
        val value: RefTree?
    ) : RefTreeNode()

    data class CommonObject(
        override val dominatorField: RefField,
        val refTree: RefTree?
    ) : RefTreeNode()
}

data class RefTree(
    val id: Long,
    val instance: Instance?,
    val children: List<RefTreeNode>
)

fun createGcRootRefGraph(linkedRecords: HprofRecordsLinked): RefTree {
    val gcRoots = linkedRecords.rootsDic.filter { it.value !is HprofRecord.RootJavaFrameRecord }
    val instanceDic = linkedRecords.instancesDic
    val rootTree = RefTree(
        0L,
        null,
        mutableListOf()
    )
    val visitTracker = HashMap<Long, RefTree?>()
    val toVisitQueue = ArrayDeque<RefTreeNode>(instanceDic.size / 2)

    // enqueue all gc roots
    val rootTreeChildren = rootTree.children as MutableList<RefTreeNode>
    for (r in gcRoots) {
        val i = instanceDic[r.key]
        if (i != null) {
            val children = mutableListOf<RefTreeNode>()
            val refTreeNode = RefTreeNode.GCRoot(
                refTree = RefTree(
                    id = r.key,
                    instance = i,
                    children = children
                )
            )
            val previousNode = visitTracker.put(r.key, refTreeNode.refTree)
            if (previousNode == null) {
                rootTreeChildren.add(refTreeNode)
                if (i is Instance.ObjectInstance && linkedRecords.isThreadInstance(i.id)) {
                    val activeThread = linkedRecords.queryThread(i.id)
                    if (activeThread != null) {
                        for (f in activeThread.frames) {
                            val frameTreeNode = RefTreeNode.GCRoot(
                                refTree = RefTree(
                                    id = f.id,
                                    instance = f.refInstance,
                                    children = mutableListOf()
                                )
                            )
                            val fp = visitTracker.put(f.id, frameTreeNode.refTree)
                            children.add(frameTreeNode)
                            if (fp == null) {
                                toVisitQueue.addLast(frameTreeNode)
                            }
                        }
                    }
                } else {
                    toVisitQueue.addLast(refTreeNode)
                }
            }
        }
    }

    while(toVisitQueue.isNotEmpty()) {
        val n = toVisitQueue.pollFirst()
        if (n != null) {
            visitTreeNode(linkedRecords, n, toVisitQueue, visitTracker)
        }
    }

    return rootTree
}

private fun visitTreeNode(
    linkedRecords: HprofRecordsLinked,
    node: RefTreeNode,
    toVisitDeque: Deque<RefTreeNode>,
    visitTracker: MutableMap<Long, RefTree?>
) {
    val instanceDic = linkedRecords.instancesDic
    val dominatorField: RefField = node.dominatorField
    val visitInstance: Instance?
    val visitInstanceChildren: MutableList<RefTreeNode>?
    when (node) {
        is RefTreeNode.CommonObject -> {
            visitInstance = node.refTree?.instance
            visitInstanceChildren = node.refTree?.children as? MutableList<RefTreeNode>
        }
        is RefTreeNode.GCRoot -> {
            visitInstance = node.refTree?.instance
            visitInstanceChildren = node.refTree?.children as? MutableList<RefTreeNode>
        }
        is RefTreeNode.ObjectArrayElement -> {
            visitInstance = node.value?.instance
            visitInstanceChildren = node.value?.children as? MutableList<RefTreeNode>
        }
        is RefTreeNode.Primitive -> {
            visitInstance = null
            visitInstanceChildren = null
        }
        is RefTreeNode.PrimitiveArray -> {
            visitInstance = null
            visitInstanceChildren = null
        }
    }
    if (visitInstance == null || visitInstanceChildren == null) {
        return
    }

    fun handleField(f: Any, isMemberField: Boolean) {
        val memberField = f as? MemberFieldAndValue
        val staticField = f as? StaticField
        val value = if (isMemberField) {
            memberField!!.value
        } else {
            staticField!!.value
        }
        when (value) {
            is ValueHolder.BooleanHolder -> {
                visitInstanceChildren.add(
                    RefTreeNode.Primitive(
                        dominatorField = if (isMemberField) RefField.MemberField(memberField!!.field) else RefField.StaticField(
                            staticField!!
                        ),
                        value = value.value
                    )
                )
            }
            is ValueHolder.ByteHolder -> {
                visitInstanceChildren.add(
                    RefTreeNode.Primitive(
                        dominatorField = if (isMemberField) RefField.MemberField(memberField!!.field) else RefField.StaticField(
                            staticField!!
                        ),
                        value = value.value
                    )
                )
            }
            is ValueHolder.CharHolder -> {
                visitInstanceChildren.add(
                    RefTreeNode.Primitive(
                        dominatorField = if (isMemberField) RefField.MemberField(memberField!!.field) else RefField.StaticField(
                            staticField!!
                        ),
                        value = value.value
                    )
                )
            }
            is ValueHolder.DoubleHolder -> {
                visitInstanceChildren.add(
                    RefTreeNode.Primitive(
                        dominatorField = if (isMemberField) RefField.MemberField(memberField!!.field) else RefField.StaticField(
                            staticField!!
                        ),
                        value = value.value
                    )
                )
            }
            is ValueHolder.FloatHolder -> {
                visitInstanceChildren.add(
                    RefTreeNode.Primitive(
                        dominatorField = if (isMemberField) RefField.MemberField(memberField!!.field) else RefField.StaticField(
                            staticField!!
                        ),
                        value = value.value
                    )
                )
            }
            is ValueHolder.IntHolder -> {
                visitInstanceChildren.add(
                    RefTreeNode.Primitive(
                        dominatorField = if (isMemberField) RefField.MemberField(memberField!!.field) else RefField.StaticField(
                            staticField!!
                        ),
                        value = value.value
                    )
                )
            }
            is ValueHolder.LongHolder -> {
                visitInstanceChildren.add(
                    RefTreeNode.Primitive(
                        dominatorField = if (isMemberField) RefField.MemberField(memberField!!.field) else RefField.StaticField(
                            staticField!!
                        ),
                        value = value.value
                    )
                )
            }
            is ValueHolder.ShortHolder -> {
                visitInstanceChildren.add(
                    RefTreeNode.Primitive(
                        dominatorField = if (isMemberField) RefField.MemberField(memberField!!.field) else RefField.StaticField(
                            staticField!!
                        ),
                        value = value.value
                    )
                )
            }
            is ValueHolder.ReferenceHolder -> {
                val i = instanceDic[value.value]
                if (i == null) {
                    visitInstanceChildren.add(
                        RefTreeNode.CommonObject(
                            dominatorField = if (isMemberField) RefField.MemberField(memberField!!.field) else RefField.StaticField(
                                staticField!!
                            ),
                            refTree = null
                        )
                    )
                } else {
                    when (i) {
                        is Instance.BoolArrayInstance -> {
                            visitInstanceChildren.add(
                                RefTreeNode.PrimitiveArray(
                                    dominatorField = if (isMemberField) RefField.MemberField(
                                        memberField!!.field
                                    ) else RefField.StaticField(staticField!!),
                                    array = i.array
                                )
                            )
                        }
                        is Instance.ByteArrayInstance -> {
                            visitInstanceChildren.add(
                                RefTreeNode.PrimitiveArray(
                                    dominatorField = if (isMemberField) RefField.MemberField(
                                        memberField!!.field
                                    ) else RefField.StaticField(staticField!!),
                                    array = i.array
                                )
                            )
                        }
                        is Instance.CharArrayInstance -> {
                            visitInstanceChildren.add(
                                RefTreeNode.PrimitiveArray(
                                    dominatorField = if (isMemberField) RefField.MemberField(
                                        memberField!!.field
                                    ) else RefField.StaticField(staticField!!),
                                    array = i.array
                                )
                            )
                        }
                        is Instance.DoubleArrayInstance -> {
                            visitInstanceChildren.add(
                                RefTreeNode.PrimitiveArray(
                                    dominatorField = if (isMemberField) RefField.MemberField(
                                        memberField!!.field
                                    ) else RefField.StaticField(staticField!!),
                                    array = i.array
                                )
                            )
                        }
                        is Instance.FloatArrayInstance -> {
                            visitInstanceChildren.add(
                                RefTreeNode.PrimitiveArray(
                                    dominatorField = if (isMemberField) RefField.MemberField(
                                        memberField!!.field
                                    ) else RefField.StaticField(staticField!!),
                                    array = i.array
                                )
                            )
                        }
                        is Instance.IntArrayInstance -> {
                            visitInstanceChildren.add(
                                RefTreeNode.PrimitiveArray(
                                    dominatorField = if (isMemberField) RefField.MemberField(
                                        memberField!!.field
                                    ) else RefField.StaticField(staticField!!),
                                    array = i.array
                                )
                            )
                        }
                        is Instance.LongArrayInstance -> {
                            visitInstanceChildren.add(
                                RefTreeNode.PrimitiveArray(
                                    dominatorField = if (isMemberField) RefField.MemberField(
                                        memberField!!.field
                                    ) else RefField.StaticField(staticField!!),
                                    array = i.array
                                )
                            )
                        }
                        is Instance.ShortArrayInstance -> {
                            visitInstanceChildren.add(
                                RefTreeNode.PrimitiveArray(
                                    dominatorField = if (isMemberField) RefField.MemberField(
                                        memberField!!.field
                                    ) else RefField.StaticField(staticField!!),
                                    array = i.array
                                )
                            )
                        }
                        is Instance.ClassDumpInstance -> {
                            val newNode = RefTreeNode.CommonObject(
                                dominatorField = if (isMemberField) RefField.MemberField(memberField!!.field) else RefField.StaticField(
                                    staticField!!
                                ),
                                refTree = RefTree(
                                    id = i.id,
                                    instance = i,
                                    children = mutableListOf()
                                )
                            )
                            val previous = visitTracker.put(i.id, newNode.refTree)
                            if (previous == null) {
                                toVisitDeque.addLast(newNode)
                                visitInstanceChildren.add(newNode)
                            } else {
                                visitInstanceChildren.add(newNode.copy(refTree = previous))
                            }
                        }
                        is Instance.ObjectArrayInstance -> {
                            for ((index, e) in i.elements.withIndex()) {
                                val newNode = RefTreeNode.ObjectArrayElement(
                                    dominatorField = RefField.ArrayElementField(
                                        field = if (isMemberField) RefField.MemberField(memberField!!.field) else RefField.StaticField(
                                            staticField!!
                                        ),
                                        index = index
                                    ),
                                    value = if (e != null) {
                                        RefTree(
                                            id = e.id,
                                            instance = e,
                                            children = mutableListOf()
                                        )
                                    } else {
                                        null
                                    }
                                )
                                val previous = if (e != null) {
                                    visitTracker.put(e.id, newNode.value)
                                } else {
                                    null
                                }
                                if (previous  == null) {
                                    visitInstanceChildren.add(newNode)
                                    if (e != null) {
                                        toVisitDeque.addLast(newNode)
                                    }
                                } else {
                                    visitInstanceChildren.add(newNode.copy(value = previous))
                                }
                            }
                        }
                        is Instance.ObjectInstance -> {
                            val newNode = RefTreeNode.CommonObject(
                                dominatorField = if (isMemberField) RefField.MemberField(memberField!!.field) else RefField.StaticField(
                                    staticField!!
                                ),
                                refTree = RefTree(
                                    id = i.id,
                                    instance = i,
                                    children = mutableListOf()
                                )
                            )
                            val previous = visitTracker.put(i.id, newNode.refTree)
                            if (previous == null) {
                                toVisitDeque.addLast(newNode)
                                visitInstanceChildren.add(newNode)
                            } else {
                                visitInstanceChildren.add(newNode.copy(refTree = previous))
                            }
                        }
                    }
                }
            }
        }
    }

    when (visitInstance) {

        is Instance.ObjectArrayInstance -> {
            for ((index, e) in visitInstance.elements.withIndex()) {
                val newNode = RefTreeNode.ObjectArrayElement(
                    dominatorField = RefField.ArrayElementField(
                        field = dominatorField,
                        index = index
                    ),
                    value = if (e != null) {
                        RefTree(
                            id = e.id,
                            instance = e,
                            children = mutableListOf()
                        )
                    } else {
                        null
                    }
                )
                val previous = if (e != null) {
                    visitTracker.put(e.id, newNode.value)
                } else {
                    null
                }
                if (previous  == null) {
                    visitInstanceChildren.add(newNode)
                    if (e != null) {
                        toVisitDeque.addLast(newNode)
                    }
                } else {
                    visitInstanceChildren.add(newNode.copy(value = previous))
                }
            }
        }

        is Instance.ClassDumpInstance -> {
            for (f in visitInstance.staticFields) {
                handleField(f, false)
            }
        }

        is Instance.ObjectInstance -> {
            if (!linkedRecords.isThreadInstance(visitInstance.id)) {
                for (f in visitInstance.memberFields) {
                    handleField(f, true)
                }
            }
        }

        else -> {

        }
    }
}