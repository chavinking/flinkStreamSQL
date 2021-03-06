/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtstack.flink.sql.format;

import com.dtstack.flink.sql.metric.MetricConstant;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.serialization.AbstractDeserializationSchema;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.MeterView;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * add metric for source
 * <p>
 * company: www.dtstack.com
 * author: toutian
 * create: 2019/12/24
 */
public class DeserializationMetricWrapper extends AbstractDeserializationSchema<Row> {

    private static final Logger LOG = LoggerFactory.getLogger(DeserializationMetricWrapper.class);

    private static int dataPrintFrequency = 1000;

    private DeserializationSchema<Row> deserializationSchema;

    private transient RuntimeContext runtimeContext;

    protected transient Counter dirtyDataCounter;

    /**
     * tps ransactions Per Second
     */
    protected transient Counter numInRecord;

    protected transient Meter numInRate;

    /**
     * rps Record Per Second: deserialize data and out record num
     */
    protected transient Counter numInResolveRecord;

    protected transient Meter numInResolveRate;

    protected transient Counter numInBytes;

    protected transient Meter numInBytesRate;

    public DeserializationMetricWrapper(TypeInformation<Row> typeInfo, DeserializationSchema<Row> deserializationSchema) {
        super(typeInfo);
        this.deserializationSchema = deserializationSchema;
    }

    public void initMetric() {
        dirtyDataCounter = runtimeContext.getMetricGroup().counter(MetricConstant.DT_DIRTY_DATA_COUNTER);

        numInRecord = runtimeContext.getMetricGroup().counter(MetricConstant.DT_NUM_RECORDS_IN_COUNTER);
        numInRate = runtimeContext.getMetricGroup().meter(MetricConstant.DT_NUM_RECORDS_IN_RATE, new MeterView(numInRecord, 20));

        numInBytes = runtimeContext.getMetricGroup().counter(MetricConstant.DT_NUM_BYTES_IN_COUNTER);
        numInBytesRate = runtimeContext.getMetricGroup().meter(MetricConstant.DT_NUM_BYTES_IN_RATE, new MeterView(numInBytes, 20));

        numInResolveRecord = runtimeContext.getMetricGroup().counter(MetricConstant.DT_NUM_RECORDS_RESOVED_IN_COUNTER);
        numInResolveRate = runtimeContext.getMetricGroup().meter(MetricConstant.DT_NUM_RECORDS_RESOVED_IN_RATE, new MeterView(numInResolveRecord, 20));
    }

    @Override
    public Row deserialize(byte[] message) throws IOException {
        try {
            if (numInRecord.getCount() % dataPrintFrequency == 0) {
                LOG.info("receive source data:" + new String(message, "UTF-8"));
            }
            numInRecord.inc();
            numInBytes.inc(message.length);
            beforeDeserialize();
            Row row = deserializationSchema.deserialize(message);
            afterDeserialize();
            numInResolveRecord.inc();
            return row;
        } catch (Exception e) {
            //add metric of dirty data
            if (dirtyDataCounter.getCount() % dataPrintFrequency == 0) {
                LOG.info("dirtyData: " + new String(message));
                LOG.error("data parse error", e);
            }
            dirtyDataCounter.inc();
            return null;
        }
    }

    protected void beforeDeserialize() throws IOException {
    }

    protected void afterDeserialize() throws IOException {
    }

    public DeserializationSchema<Row> getDeserializationSchema() {
        return deserializationSchema;
    }

    public RuntimeContext getRuntimeContext() {
        return runtimeContext;
    }

    public void setRuntimeContext(RuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }
}
