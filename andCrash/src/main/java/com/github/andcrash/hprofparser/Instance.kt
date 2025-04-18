package com.github.andcrash.hprofparser

data class MemberFieldAndValue(
    val field: MemberField,
    val value: ValueHolder
)

sealed class Instance {
    abstract val id: Long
    abstract val stackTrace: StackTrace?

    data class ObjectInstance(
        override val id: Long,
        override val stackTrace: StackTrace?,
        val clazz: LoadedClass?,
        val value: ByteArray,
        val memberFields: List<MemberFieldAndValue>
    ) : Instance() {

        fun getMemberField(fieldName: String): MemberFieldAndValue? {
          return memberFields.find { it.field.nameString == fieldName }
        }
    }

    data class BoolArrayInstance(
        override val id: Long,
        override val stackTrace: StackTrace?,
        val array: BooleanArray,
    ) : Instance()

    data class CharArrayInstance(
        override val id: Long,
        override val stackTrace: StackTrace?,
        val array: CharArray,
    ) : Instance()

    data class FloatArrayInstance(
        override val id: Long,
        override val stackTrace: StackTrace?,
        val array: FloatArray,
    ) : Instance()

    data class DoubleArrayInstance(
        override val id: Long,
        override val stackTrace: StackTrace?,
        val array: DoubleArray,
    ) : Instance()

    data class ByteArrayInstance(
        override val id: Long,
        override val stackTrace: StackTrace?,
        val array: ByteArray,
    ) : Instance()

    data class ShortArrayInstance(
        override val id: Long,
        override val stackTrace: StackTrace?,
        val array: ShortArray,
    ) : Instance()

    data class IntArrayInstance(
        override val id: Long,
        override val stackTrace: StackTrace?,
        val array: IntArray,
    ) : Instance()

    data class LongArrayInstance(
        override val id: Long,
        override val stackTrace: StackTrace?,
        val array: LongArray,
    ) : Instance()

    data class ObjectArrayInstance(
        override val id: Long,
        override val stackTrace: StackTrace?,
        val arrayLength: Int,
        val arrayClass: LoadedClass?,
        val elements: List<Instance?>
    ) : Instance()

    data class ClassDumpInstance(
        override val id: Long,
        override val stackTrace: StackTrace?,
        val clazz: LoadedClass?,
        val supperClass: LoadedClass?,
        val classLoader: Instance?,
        val signersId: Long,
        val protectionDomainId: Long,
        val instanceSize: Int,
        val constFields: List<ConstField>,
        val staticFields: List<StaticField>,
        val memberFields: List<MemberField>
    ) : Instance()
}