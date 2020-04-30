/*
 * Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.bigquery;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.api.services.bigquery.model.Streamingbuffer;
import com.google.api.services.bigquery.model.Table;
import com.google.api.services.bigquery.model.TableReference;
import com.google.cloud.bigquery.StandardTableDefinition.StreamingBuffer;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class StandardTableDefinitionTest {

  private static final Field FIELD_SCHEMA1 =
      Field.newBuilder("StringField", LegacySQLTypeName.STRING)
          .setMode(Field.Mode.NULLABLE)
          .setDescription("FieldDescription1")
          .build();
  private static final Field FIELD_SCHEMA2 =
      Field.newBuilder("IntegerField", LegacySQLTypeName.INTEGER)
          .setMode(Field.Mode.REPEATED)
          .setDescription("FieldDescription2")
          .build();
  private static final Field FIELD_SCHEMA3 =
      Field.newBuilder("RecordField", LegacySQLTypeName.RECORD, FIELD_SCHEMA1, FIELD_SCHEMA2)
          .setMode(Field.Mode.REQUIRED)
          .setDescription("FieldDescription3")
          .build();
  private static final Schema TABLE_SCHEMA = Schema.of(FIELD_SCHEMA1, FIELD_SCHEMA2, FIELD_SCHEMA3);
  private static final Long NUM_BYTES = 42L;
  private static final Long NUM_LONG_TERM_BYTES = 18L;
  private static final Long NUM_ROWS = 43L;
  private static final String LOCATION = "US";
  private static final StreamingBuffer STREAMING_BUFFER = new StreamingBuffer(1L, 2L, 3L);
  private static final TimePartitioning TIME_PARTITIONING =
      TimePartitioning.of(TimePartitioning.Type.DAY, 42);
  private static final Clustering CLUSTERING =
      Clustering.newBuilder().setFields(ImmutableList.of("Foo", "Bar")).build();
  private static final StandardTableDefinition TABLE_DEFINITION =
      StandardTableDefinition.newBuilder()
          .setLocation(LOCATION)
          .setNumBytes(NUM_BYTES)
          .setNumRows(NUM_ROWS)
          .setNumLongTermBytes(NUM_LONG_TERM_BYTES)
          .setStreamingBuffer(STREAMING_BUFFER)
          .setSchema(TABLE_SCHEMA)
          .setTimePartitioning(TIME_PARTITIONING)
          .setClustering(CLUSTERING)
          .build();

  @Test
  public void testToBuilder() {
    compareStandardTableDefinition(TABLE_DEFINITION, TABLE_DEFINITION.toBuilder().build());
    StandardTableDefinition tableDefinition =
        TABLE_DEFINITION.toBuilder().setLocation("EU").build();
    assertEquals("EU", tableDefinition.getLocation());
    tableDefinition = tableDefinition.toBuilder().setLocation(LOCATION).build();
    compareStandardTableDefinition(TABLE_DEFINITION, tableDefinition);
  }

  @Test
  public void testToBuilderIncomplete() {
    StandardTableDefinition tableDefinition = StandardTableDefinition.of(TABLE_SCHEMA);
    assertEquals(tableDefinition, tableDefinition.toBuilder().build());
  }

  @Test
  public void testBuilder() {
    assertEquals(TableDefinition.Type.TABLE, TABLE_DEFINITION.getType());
    assertEquals(TABLE_SCHEMA, TABLE_DEFINITION.getSchema());
    assertEquals(LOCATION, TABLE_DEFINITION.getLocation());
    assertEquals(NUM_BYTES, TABLE_DEFINITION.getNumBytes());
    assertEquals(NUM_LONG_TERM_BYTES, TABLE_DEFINITION.getNumLongTermBytes());
    assertEquals(NUM_ROWS, TABLE_DEFINITION.getNumRows());
    assertEquals(STREAMING_BUFFER, TABLE_DEFINITION.getStreamingBuffer());
    assertEquals(TIME_PARTITIONING, TABLE_DEFINITION.getTimePartitioning());
    assertEquals(CLUSTERING, TABLE_DEFINITION.getClustering());
  }

  @Test
  public void testOf() {
    StandardTableDefinition definition = StandardTableDefinition.of(TABLE_SCHEMA);
    assertEquals(TableDefinition.Type.TABLE, TABLE_DEFINITION.getType());
    assertEquals(TABLE_SCHEMA, TABLE_DEFINITION.getSchema());
    assertNull(definition.getLocation());
    assertNull(definition.getNumBytes());
    assertNull(definition.getNumLongTermBytes());
    assertNull(definition.getNumRows());
    assertNull(definition.getStreamingBuffer());
    assertNull(definition.getTimePartitioning());
    assertNull(definition.getClustering());
  }

  @Test
  public void testToAndFromPb() {
    assertTrue(TableDefinition.fromPb(TABLE_DEFINITION.toPb()) instanceof StandardTableDefinition);
    compareStandardTableDefinition(
        TABLE_DEFINITION, TableDefinition.<StandardTableDefinition>fromPb(TABLE_DEFINITION.toPb()));
    StandardTableDefinition definition = StandardTableDefinition.of(TABLE_SCHEMA);
    assertTrue(TableDefinition.fromPb(definition.toPb()) instanceof StandardTableDefinition);
    compareStandardTableDefinition(
        definition, TableDefinition.<StandardTableDefinition>fromPb(definition.toPb()));
  }

  @Test
  public void testFromPbWithUnexpectedTimePartitioningTypeRaisesInvalidArgumentException() {
    Table invalidTable =
        new Table()
            .setType("TABLE")
            .setTableReference(
                new TableReference()
                    .setProjectId("ILLEGAL_ARG_TEST_PROJECT")
                    .setDatasetId("ILLEGAL_ARG_TEST_DATASET")
                    .setTableId("ILLEGAL_ARG_TEST_TABLE"))
            .setTimePartitioning(
                new com.google.api.services.bigquery.model.TimePartitioning().setType("GHURRY"));
    try {
      StandardTableDefinition.fromPb(invalidTable);
    } catch (IllegalArgumentException ie) {
      assertThat(
          ie.getMessage(),
          allOf(
              containsString("Illegal Argument - Got unexpected time partitioning"),
              containsString("GHURRY"),
              containsString("ILLEGAL_ARG_TEST_PROJECT"),
              containsString("ILLEGAL_ARG_TEST_DATASET"),
              containsString("ILLEGAL_ARG_TEST_TABLE")));
      return;
    }
    fail("testFromPb illegal argument exception did not throw!");
  }

  /*
  @Test
  public void testFromPbWithNullTimePartitioningTypeRaisesNullPointerException() {
    Table invalidTable =
        new Table()
            .setType("TABLE")
            .setTableReference(
                new TableReference()
                    .setProjectId("NULL_PTR_TEST_PROJECT")
                    .setDatasetId("NULL_PTR_TEST_DATASET")
                    .setTableId("NULL_PTR_TEST_TABLE"))
            .setTimePartitioning(
                new com.google.api.services.bigquery.model.TimePartitioning().setType(null));
    try {
      StandardTableDefinition.fromPb(invalidTable);
    } catch (NullPointerException ne) {
      assertThat(
          ne.getMessage(),
          allOf(
              containsString("Null pointer - Got unexpected time partitioning"),
              containsString("null"),
              containsString("NULL_PTR_TEST_PROJECT"),
              containsString("NULL_PTR_TEST_DATASET"),
              containsString("NULL_PTR_TEST_TABLE")));
      return;
    }
    fail("testFromPb null pointer exception did not throw!");
  }
  */

  @Test
  public void testFromPbWithNullEstimatedRowsAndBytes() {
    StandardTableDefinition.fromPb(
        TABLE_DEFINITION.toPb().setStreamingBuffer(new Streamingbuffer()));
  }

  @Test
  public void testStreamingBufferWithNullFieldsToPb() {
    new StreamingBuffer(null, null, null).toPb();
  }

  private void compareStandardTableDefinition(
      StandardTableDefinition expected, StandardTableDefinition value) {
    assertEquals(expected, value);
    assertEquals(expected.getSchema(), value.getSchema());
    assertEquals(expected.getType(), value.getType());
    assertEquals(expected.getNumBytes(), value.getNumBytes());
    assertEquals(expected.getNumLongTermBytes(), value.getNumLongTermBytes());
    assertEquals(expected.getNumRows(), value.getNumRows());
    assertEquals(expected.getLocation(), value.getLocation());
    assertEquals(expected.getStreamingBuffer(), value.getStreamingBuffer());
    assertEquals(expected.getType(), value.getType());
    assertEquals(expected.getTimePartitioning(), value.getTimePartitioning());
    assertEquals(expected.getClustering(), value.getClustering());
    assertEquals(expected.hashCode(), value.hashCode());
  }
}
