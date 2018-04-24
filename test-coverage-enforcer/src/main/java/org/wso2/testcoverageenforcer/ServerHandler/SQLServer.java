/*
 *   Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.testcoverageenforcer.ServerHandler;

import org.wso2.testcoverageenforcer.Constants;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Represent a SQL server containing a specific table with wso2 repository information
 * (Based on pqd_product_area_component_mapping sql dump)
 */
public class SQLServer {

    /**
     * SQL table containing urls to the GitHub repositories
     */
    private ResultSet repositoryTableData;
    /**
     * SQL table column ID for repository urls
     */
    private String sqlRepositoryColumn;
    /**
     * connection with the SQL server
     */
    private Connection m_connection;

    /**
     * Read required table containing repository urls from the server and cache it
     *
     * @param propertiesFilePath File path to the properties file
     * @throws SQLException Error in SQL connection
     * @throws IOException  Error opening properties file
     */
    public SQLServer(String propertiesFilePath) throws SQLException, IOException {

        Properties properties = new Properties();
        properties.load(new FileInputStream(propertiesFilePath));
        this.sqlRepositoryColumn = properties.getProperty(Constants.Sql.SQL_REPO_COLUMN);

        this.m_connection = DriverManager.getConnection(
                properties.getProperty(Constants.Sql.SQL_URL),
                properties.getProperty(Constants.Sql.SQL_USERNAME),
                properties.getProperty(Constants.Sql.SQL_PASSWORD));
        PreparedStatement stat = this.m_connection.prepareStatement("SELECT "
                        + properties.getProperty(Constants.Sql.SQL_REPO_COLUMN) + " FROM "
                        + properties.getProperty(Constants.Sql.SQL_TABLE),
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stat.setFetchSize(Integer.MIN_VALUE);
        this.repositoryTableData = stat.executeQuery();
    }

    /**
     * Get next repository url from the cached repository table if available
     *
     * @return Repository url if available. Otherwise a null value is returned.
     * @throws SQLException Error in SQL connection
     */
    public String getNextRepositoryURL() throws SQLException {

        if (this.repositoryTableData.next()) {
            return this.repositoryTableData.getString(sqlRepositoryColumn).replace(Constants.Git.GITHUB_URL, Constants.EMPTY_STRING);
        } else {
            return null;
        }
    }

    /**
     * Close the sql connection
     *
     * @throws SQLException Error in SQL connection
     */
    public void close() throws SQLException {

        this.m_connection.close();
    }
}
