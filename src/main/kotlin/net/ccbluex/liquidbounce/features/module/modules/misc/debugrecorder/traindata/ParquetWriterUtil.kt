package net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.traindata

import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import java.io.File
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.Configurator

object ParquetWriterUtil {

    private val schema: Schema = SchemaBuilder.record("TrainDataSample")
        .namespace("net.ccbluex.liquidbounce.traindata")
        .fields()
        .name("timestamp").type().longType().noDefault()
        .name("playerId").type().stringType().noDefault()
        .name("posX").type().doubleType().noDefault()
        .name("posY").type().doubleType().noDefault()
        .name("posZ").type().doubleType().noDefault()
        .name("yaw").type().floatType().noDefault()
        .name("pitch").type().floatType().noDefault()
        .name("isSneaking").type().booleanType().noDefault()
        .name("isOnGround").type().booleanType().noDefault()
        .name("isUsingItem").type().booleanType().noDefault()
        .name("isSwinging").type().booleanType().noDefault()
        .name("wasHit").type().booleanType().noDefault()
        .name("closestArrowX").type().doubleType().noDefault()
        .name("closestArrowY").type().doubleType().noDefault()
        .name("closestArrowZ").type().doubleType().noDefault()
        .name("mainHandCategory").type().intType().noDefault()
        .name("offHandCategory").type().intType().noDefault()
        .name("floorMap").type().array().items().intType().noDefault()
        .name("ceilMap").type().array().items().intType().noDefault()
        .name("poiMap").type().array().items().intType().noDefault()
        .endRecord()

    init {
        // Disable annoying avro/parquet logging about missing arguments
        try {
            Configurator.setLevel("org.apache.parquet", Level.ERROR)
            Configurator.setLevel("org.apache.hadoop", Level.ERROR)
        } catch (_: Exception) {
            // Ignore if log4j is not the backing logger
        }
    }

    fun saveToParquet(data: List<DataSample>, file: File) {
        val conf = Configuration()
        
        // Suppress warning from hadoop as well
        conf.setQuietMode(true)
        
        val path = Path("file:///${file.absolutePath}")
        
        val writer = AvroParquetWriter.builder<GenericRecord>(path)
            .withSchema(schema)
            .withConf(conf)
            .withCompressionCodec(CompressionCodecName.ZSTD)
            .build()

        try {
            for (sample in data) {
                val record = GenericData.Record(schema)
                record.put("timestamp", sample.timestamp)
                record.put("playerId", sample.playerId)
                record.put("posX", sample.posX)
                record.put("posY", sample.posY)
                record.put("posZ", sample.posZ)
                record.put("yaw", sample.yaw)
                record.put("pitch", sample.pitch)
                record.put("isSneaking", sample.isSneaking)
                record.put("isOnGround", sample.isOnGround)
                record.put("isUsingItem", sample.isUsingItem)
                record.put("isSwinging", sample.isSwinging)
                record.put("wasHit", sample.wasHit)
                record.put("closestArrowX", sample.closestArrowX)
                record.put("closestArrowY", sample.closestArrowY)
                record.put("closestArrowZ", sample.closestArrowZ)
                record.put("mainHandCategory", sample.mainHandCategory)
                record.put("offHandCategory", sample.offHandCategory)
                
                // Avro doesn't have a native short array type, so we store as Int list
                record.put("floorMap", sample.floorMap.map { it.toInt() })
                record.put("ceilMap", sample.ceilMap.map { it.toInt() })
                record.put("poiMap", sample.poiMap.toList())

                writer.write(record)
            }
        } finally {
            writer.close()
        }
    }
}
