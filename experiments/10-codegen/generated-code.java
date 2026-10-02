/* 001 */ public Object generate(Object[] references) {
/* 002 */   return new GeneratedIteratorForCodegenStage1(references);
/* 003 */ }
/* 004 */
/* 005 */ // codegenStageId=1
/* 006 */ final class GeneratedIteratorForCodegenStage1 extends org.apache.spark.sql.execution.BufferedRowIterator {
/* 007 */   private Object[] references;
/* 008 */   private scala.collection.Iterator[] inputs;
/* 009 */   private boolean range_initRange_0;
/* 010 */   private long range_nextIndex_0;
/* 011 */   private TaskContext range_taskContext_0;
/* 012 */   private InputMetrics range_inputMetrics_0;
/* 013 */   private long range_batchEnd_0;
/* 014 */   private long range_numElementsTodo_0;
/* 015 */   private org.apache.spark.sql.catalyst.expressions.codegen.UnsafeRowWriter[] range_mutableStateArray_0 = new org.apache.spark.sql.catalyst.expressions.codegen.UnsafeRowWriter[3];
/* 016 */
/* 017 */   public GeneratedIteratorForCodegenStage1(Object[] references) {
/* 018 */     this.references = references;
/* 019 */   }
/* 020 */
/* 021 */   public void init(int index, scala.collection.Iterator[] inputs) {
/* 022 */     partitionIndex = index;
/* 023 */     this.inputs = inputs;
/* 024 */
/* 025 */     range_taskContext_0 = TaskContext.get();
/* 026 */     range_inputMetrics_0 = range_taskContext_0.taskMetrics().inputMetrics();
/* 027 */     range_mutableStateArray_0[0] = new org.apache.spark.sql.catalyst.expressions.codegen.UnsafeRowWriter(1, 0);
/* 028 */     range_mutableStateArray_0[1] = new org.apache.spark.sql.catalyst.expressions.codegen.UnsafeRowWriter(1, 0);
/* 029 */     range_mutableStateArray_0[2] = new org.apache.spark.sql.catalyst.expressions.codegen.UnsafeRowWriter(1, 0);
/* 030 */
/* 031 */   }
/* 032 */
/* 033 */   private void range_initRange_1(int idx) {
/* 034 */     java.math.BigInteger index = java.math.BigInteger.valueOf(idx);
/* 035 */     java.math.BigInteger numSlice = java.math.BigInteger.valueOf(1L);
/* 036 */     java.math.BigInteger numElement = java.math.BigInteger.valueOf(1000L);
/* 037 */     java.math.BigInteger step = java.math.BigInteger.valueOf(1L);
/* 038 */     java.math.BigInteger start = java.math.BigInteger.valueOf(0L);
/* 039 */     long partitionEnd;
/* 040 */
/* 041 */     java.math.BigInteger st = index.multiply(numElement).divide(numSlice).multiply(step).add(start);
/* 042 */     if (st.compareTo(java.math.BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
/* 043 */       range_nextIndex_0 = Long.MAX_VALUE;
/* 044 */     } else if (st.compareTo(java.math.BigInteger.valueOf(Long.MIN_VALUE)) < 0) {
/* 045 */       range_nextIndex_0 = Long.MIN_VALUE;
/* 046 */     } else {
/* 047 */       range_nextIndex_0 = st.longValue();
/* 048 */     }
/* 049 */     range_batchEnd_0 = range_nextIndex_0;
/* 050 */
/* 051 */     java.math.BigInteger end = index.add(java.math.BigInteger.ONE).multiply(numElement).divide(numSlice)
/* 052 */     .multiply(step).add(start);
/* 053 */     if (end.compareTo(java.math.BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
/* 054 */       partitionEnd = Long.MAX_VALUE;
/* 055 */     } else if (end.compareTo(java.math.BigInteger.valueOf(Long.MIN_VALUE)) < 0) {
/* 056 */       partitionEnd = Long.MIN_VALUE;
/* 057 */     } else {
/* 058 */       partitionEnd = end.longValue();
/* 059 */     }
/* 060 */
/* 061 */     java.math.BigInteger startToEnd = java.math.BigInteger.valueOf(partitionEnd).subtract(
/* 062 */       java.math.BigInteger.valueOf(range_nextIndex_0));
/* 063 */     range_numElementsTodo_0  = startToEnd.divide(step).longValue();
/* 064 */     if (range_numElementsTodo_0 < 0) {
/* 065 */       range_numElementsTodo_0 = 0;
/* 066 */     } else if (startToEnd.remainder(step).compareTo(java.math.BigInteger.valueOf(0L)) != 0) {
/* 067 */       range_numElementsTodo_0++;
/* 068 */     }
/* 069 */   }
/* 070 */
/* 071 */   protected void processNext() throws java.io.IOException {
/* 072 */     // initialize Range
/* 073 */     if (!range_initRange_0) {
/* 074 */       range_initRange_0 = true;
/* 075 */       range_initRange_1(partitionIndex);
/* 076 */     }
/* 077 */
/* 078 */     while (true) {
/* 079 */       if (range_nextIndex_0 == range_batchEnd_0) {
/* 080 */         long range_nextBatchTodo_0;
/* 081 */         if (range_numElementsTodo_0 > 1000L) {
/* 082 */           range_nextBatchTodo_0 = 1000L;
/* 083 */           range_numElementsTodo_0 -= 1000L;
/* 084 */         } else {
/* 085 */           range_nextBatchTodo_0 = range_numElementsTodo_0;
/* 086 */           range_numElementsTodo_0 = 0;
/* 087 */           if (range_nextBatchTodo_0 == 0) break;
/* 088 */         }
/* 089 */         range_batchEnd_0 += range_nextBatchTodo_0 * 1L;
/* 090 */       }
/* 091 */
/* 092 */       int range_localEnd_0 = (int)((range_batchEnd_0 - range_nextIndex_0) / 1L);
/* 093 */       for (int range_localIdx_0 = 0; range_localIdx_0 < range_localEnd_0; range_localIdx_0++) {
/* 094 */         long range_value_0 = ((long)range_localIdx_0 * 1L) + range_nextIndex_0;
/* 095 */
/* 096 */         do {
/* 097 */           boolean filter_isNull_0 = true;
/* 098 */           boolean filter_value_0 = false;
/* 099 */           boolean filter_isNull_1 = false;
/* 100 */           long filter_value_1 = -1L;
/* 101 */           if (3L == 0) {
/* 102 */             throw QueryExecutionErrors.remainderByZeroError(((org.apache.spark.sql.catalyst.trees.SQLQueryContext) references[3] /* errCtx */));
/* 103 */           } else {
/* 104 */             filter_value_1 = (long)(range_value_0 % 3L);
/* 105 */           }
/* 106 */           if (!filter_isNull_1) {
/* 107 */             filter_isNull_0 = false; // resultCode could change nullability.
/* 108 */             filter_value_0 = filter_value_1 == 0L;
/* 109 */
/* 110 */           }
/* 111 */           if (filter_isNull_0 || !filter_value_0) continue;
/* 112 */
/* 113 */           ((org.apache.spark.sql.execution.metric.SQLMetric) references[1] /* numOutputRows */).add(1);
/* 114 */
/* 115 */           // common sub-expressions
/* 116 */
/* 117 */           long project_value_0 = -1L;
/* 118 */
/* 119 */           project_value_0 = org.apache.spark.sql.catalyst.util.MathUtils.multiplyExact(range_value_0, 2L, ((org.apache.spark.sql.catalyst.trees.SQLQueryContext) references[4] /* errCtx */));
/* 120 */           range_mutableStateArray_0[2].reset();
/* 121 */
/* 122 */           range_mutableStateArray_0[2].write(0, project_value_0);
/* 123 */           append((range_mutableStateArray_0[2].getRow()));
/* 124 */
/* 125 */         } while (false);
/* 126 */
/* 127 */         if (shouldStop()) {
/* 128 */           range_nextIndex_0 = range_value_0 + 1L;
/* 129 */           ((org.apache.spark.sql.execution.metric.SQLMetric) references[0] /* numOutputRows */).add(range_localIdx_0 + 1);
/* 130 */           range_inputMetrics_0.incRecordsRead(range_localIdx_0 + 1);
/* 131 */           return;
/* 132 */         }
/* 133 */
/* 134 */       }
/* 135 */       range_nextIndex_0 = range_batchEnd_0;
/* 136 */       ((org.apache.spark.sql.execution.metric.SQLMetric) references[0] /* numOutputRows */).add(range_localEnd_0);
/* 137 */       range_inputMetrics_0.incRecordsRead(range_localEnd_0);
/* 138 */       range_taskContext_0.killTaskIfInterrupted();
/* 139 */     }
/* 140 */   }
/* 141 */
/* 142 */ }
