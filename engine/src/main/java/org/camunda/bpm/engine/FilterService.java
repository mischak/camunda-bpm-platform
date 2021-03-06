/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine;

import java.util.List;

import org.camunda.bpm.engine.filter.Filter;
import org.camunda.bpm.engine.filter.FilterQuery;
import org.camunda.bpm.engine.query.Query;


/**
 * @author Sebastian Menski
 */
public interface FilterService {

  /**
   * Creates a new filter.
   *
   * @return a new filter
   */
  Filter newFilter();

  /**
   * Creates a new filter with a given name.
   *
   * @return a new filter with a name
   */
  Filter newFilter(String filterName);

  /**
   * Creates a new task filter.
   *
   * @return a new task filter
   */
  Filter newTaskFilter();

  /**
   * Creates a new task filter with a given name.
   *
   * @return a new task filter with a name
   */
  Filter newTaskFilter(String filterName);

  /**
   * Creates a new filter query
   *
   * @return a new query for filters
   */
  FilterQuery createFilterQuery();


  /**
   * Creates a new task filter query.
   *
   * @return a new query for task filters
   */
  FilterQuery createTaskFilterQuery();

  /**
   * Saves the filter in the database.
   *
   * @param filter the filter to save
   * @return return the saved filter
   */
  Filter saveFilter(Filter filter);

  /**
   * Returns the filter for the given filter id.
   *
   * @param filterId the id of the filter
   * @return the filter
   */
  Filter getFilter(String filterId);

  /**
   * Deletes a filter by its id.
   *
   * @param filterId the id of the filter
   * @return the deleted filter
   */
  Filter deleteFilter(String filterId);

  /**
   * Executes the query of the filter and returns the result as list.
   *
   * @param filterId the the id of the filter
   * @return the query result as list
   */
  <T> List<T> list(String filterId);

  /**
   * Executes the extended query of a filter and returns the result as list.
   *
   * @param filterId the id of the filter
   * @param extendingQuery additional query to extend the filter query
   * @return the query result as list
   */
  <T> List<T> list(String filterId, Query extendingQuery);

  /**
   * Executes the extended query of a filter and returns the result as list.
   *
   * @param filterId the id of the filter
   * @param extendingQuery additional query to extend the filter query as JSON object
   * @return the query result as list
   */
  <T> List<T> list(String filterId, String extendingQuery);

  /**
   * Executes the query of the filter and returns the result in the given boundaries as list.
   *
   * @param filterId the the id of the filter
   * @param firstResult first result to select
   * @param maxResults maximal number of results
   * @return the query result as list
   */
  <T> List<T> listPage(String filterId, int firstResult, int maxResults);

  /**
   * Executes the extended query of a filter and returns the result in the given boundaries as list.
   *
   * @param extendingQuery additional query to extend the filter query
   * @param filterId the id of the filter
   * @param firstResult first result to select
   * @param maxResults maximal number of results
   * @return the query result as list
   */
  <T> List<T> listPage(String filterId, Query extendingQuery, int firstResult, int maxResults);

  /**
   * Executes the extended query of a filter and returns the result in the given boundaries as list.
   *
   * @param extendingQuery additional query to extend the filter query
   * @param filterId the id of the filter
   * @param firstResult first result to select
   * @param maxResults maximal number of results
   * @return the query result as list
   */
  <T> List<T> listPage(String filterId, String extendingQuery, int firstResult, int maxResults);

  /**
   * Executes the query of the filter and returns the a single result.
   *
   * @param filterId the the id of the filter
   * @return the single query result
   */
  <T> T singleResult(String filterId);

  /**
   * Executes the extended query of the filter and returns the a single result.
   *
   * @param filterId the the id of the filter
   * @param extendingQuery additional query to extend the filter query
   * @return the single query result
   */
  <T> T singleResult(String filterId, Query extendingQuery);

  /**
   * Executes the extended query of the filter and returns the a single result.
   *
   * @param filterId the the id of the filter
   * @param extendingQuery additional query to extend the filter query as JSON object
   * @return the single query result
   */
  <T> T singleResult(String filterId, String extendingQuery);

  /**
   * Executes the query of the filter and returns the result count.
   *
   * @param filterId the the id of the filter
   * @return the result count
   */
  Long count(String filterId);

  /**
   * Executes the extended query of the filter and returns the result count.
   *
   * @param filterId the the id of the filter
   * @param extendingQuery additional query to extend the filter query as JSON object
   * @return the result count
   */
  Long count(String filterId, Query extendingQuery);

  /**
   * Executes the extended query of the filter and returns the result count.
   *
   * @param filterId the the id of the filter
   * @param extendingQuery additional query to extend the filter query as JSON object
   * @return the result count
   */
  Long count(String filterId, String extendingQuery);

}
