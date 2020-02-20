/*
 * Copyright (c) 2020 Alexander Iskander
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package com.skanders.rms.util.connectionpool;

import com.skanders.rms.def.verify.RMSVerify;
import com.skanders.rms.def.logger.Log;
import com.skanders.rms.util.result.Resulted;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.validation.constraints.NotNull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SQLQuery
{
    private static final Logger LOG = LogManager.getLogger(SQLQuery.class);

    private final String query;
    private final PoolManager poolManager;
    private final List<ParamPair> paramList;

    private boolean closed;

    private SQLQuery(@NotNull String query, @NotNull PoolManager poolManager)
    {
        RMSVerify.checkNull(query, "query cannot be null.");
        RMSVerify.checkNull(poolManager, "poolManager cannot be null.");

        this.query       = query;
        this.poolManager = poolManager;
        this.paramList   = new ArrayList<>();
        this.closed      = false;
    }

    public static SQLQuery createQuery(@NotNull String query, @NotNull PoolManager poolManager)
    {
        return new SQLQuery(query, poolManager);
    }

    public void set(int type, Object param)
    {
        paramList.add(new ParamPair(type, param));
    }

    public Resulted<Integer> executeUpdate()
    {
        RMSVerify.argument(closed, "SQLQuery cannot be called after closed");
        this.closed = true;

        LOG.debug(Log.ENTER, "Database Execute Update");

        try (QueryManager queryManager = poolManager.createQuery(query))
        {
            LOG.trace(Log.ATTEMPT, "Setting Query Parameters");
            queryManager.setParams(paramList);
            LOG.trace(Log.ATTEMPT_DONE, "Setting Query Parameters");

            LOG.trace(Log.ATTEMPT, "Execute Update Query");
            Integer updateCount = queryManager.executeUpdate();
            LOG.trace(Log.ATTEMPT_DONE, "Execute Update Query");

            return Resulted.inValue(updateCount);

        } catch (SQLException e) {
            LOG.error(Log.EXIT_FAIL, "Prepare Database Update Execution", e.getClass(), e.getMessage());

            return Resulted.inException(e);

        }
    }

    public Resulted<SQLResult> executeQuery()
    {
        RMSVerify.argument(closed, "SQLQuery cannot be called after closed");
        this.closed = true;

        LOG.debug(Log.ENTER, "Database Execute Query");

        QueryManager queryManager = null;

        try {
            queryManager = poolManager.createQuery(query);

            LOG.trace(Log.ATTEMPT, "Setting Query Parameters");
            queryManager.setParams(paramList);
            LOG.trace(Log.ATTEMPT_DONE, "Setting Query Parameters");

            LOG.trace(Log.ATTEMPT, "Execute Query");
            ResultSet rs = queryManager.executeQuery();
            LOG.trace(Log.ATTEMPT_DONE, "Execute Query");
            return Resulted.inValue(SQLResult.newInstance(queryManager, rs));

        } catch (SQLException e) {
            LOG.error(Log.EXIT_FAIL, "Prepare Database Query Execution", e.getClass(), e.getMessage());

            RMSVerify.close(queryManager);

            return Resulted.inException(e);

        }
    }
}
