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

package com.dtstack.flink.sql.side.kingbase.table;

import com.dtstack.flink.sql.side.AbstractSideTableInfo;
import com.dtstack.flink.sql.side.kingbase.KingbaseAllSideInfo;
import com.dtstack.flink.sql.side.rdb.all.AbstractRdbTableFunction;
import com.dtstack.flink.sql.util.DtStringUtil;
import org.apache.flink.shaded.guava18.com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;

/**
 * @author: chuixue
 * @create: 2020-10-27 14:21
 * @description:
 **/
public class KingbaseTableFunction extends AbstractRdbTableFunction {

    private static final long serialVersionUID = 2021683212163965319L;

    private static final Logger LOG = LoggerFactory.getLogger(KingbaseTableFunction.class);

    private static final String KINGBASE_DRIVER = "com.kingbase8.Driver";

    public KingbaseTableFunction(AbstractSideTableInfo sideTableInfo, String[] lookupKeys) {
        super(new KingbaseAllSideInfo(sideTableInfo, lookupKeys));
    }

    @Override
    public Connection getConn(String dbUrl, String userName, String password) {
        try {
            Class.forName(KINGBASE_DRIVER);
            //add param useCursorFetch=true
            Map<String, String> addParams = Maps.newHashMap();
            addParams.put("useCursorFetch", "true");
            String targetDbUrl = DtStringUtil.addJdbcParam(dbUrl, addParams, true);
            return DriverManager.getConnection(targetDbUrl, userName, password);
        } catch (Exception e) {
            LOG.error("kingbase get connection error", e);
            throw new RuntimeException("kingbase get connect error", e);
        }
    }
}
