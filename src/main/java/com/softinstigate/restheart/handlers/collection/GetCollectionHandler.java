/*
 * Copyright SoftInstigate srl. All Rights Reserved.
 *
 *
 * The copyright to the computer program(s) herein is the property of
 * SoftInstigate srl, Italy. The program(s) may be used and/or copied only
 * with the written permission of SoftInstigate srl or in accordance with the
 * terms and conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied. This copyright notice must not be removed.
 */
package com.softinstigate.restheart.handlers.collection;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.softinstigate.restheart.handlers.GetHandler;
import com.softinstigate.restheart.utils.RequestContext;
import io.undertow.server.HttpServerExchange;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author uji
 */
public class GetCollectionHandler extends GetHandler
{
    private static final Logger logger = LoggerFactory.getLogger(GetCollectionHandler.class);

    /**
     * Creates a new instance of EntityResource
     */
    public GetCollectionHandler()
    {
    }

    @Override
    protected String generateContent(HttpServerExchange exchange, MongoClient client, int page, int pagesize, Deque<String> sortBy, Deque<String> filterBy, Deque<String> filter)
    {
        RequestContext rc = new RequestContext(exchange);

        DBCollection coll = client.getDB(rc.getDBName()).getCollection(rc.getCollectionName());

        // apply sort_by
        DBObject sort = new BasicDBObject();

        if (sortBy == null || sortBy.isEmpty())
        {
            sort.put("_id", 1);
        }
        else
        {
            sortBy.stream().forEach((sf) ->
            {
                if (sf.startsWith("-"))
                {
                    sort.put(sf.substring(1), -1);
                }
                else if (sf.startsWith("+"))
                {
                    sort.put(sf.substring(1), -1);
                }
                else
                {
                    sort.put(sf, 1);
                }
            });
        }

        // apply filter_by and filter
        logger.warn("filter not yet implemented");

        long size = coll.count();

        DBCursor cursor = coll.find().sort(sort).limit(pagesize).skip(pagesize * (page - 1));
        
        ArrayList<DBObject> rows = new ArrayList<>(cursor.toArray());
        
        List<Map<String, Object>> data = new ArrayList<>();

        rows.stream().map((row) ->
        {
            TreeMap<String, Object> properties = new TreeMap<>();

            row.keySet().stream().forEach((key) ->
            {
                // data value is either a String or a Map. the former case applies with nested json objects

                Object obj = row.get(key);

                if (obj instanceof BasicDBList)
                {
                    BasicDBList dblist = (BasicDBList) obj;

                    obj = dblist.toMap();
                }

                properties.put(key, obj);
            });

            return properties;
        }).forEach((item) ->
        {
            data.add(item);
        });

        return generateCollectionContent(exchange.getRequestURL(), exchange.getQueryString(), data, page, pagesize, size, sortBy, filterBy, filter);
    }
}