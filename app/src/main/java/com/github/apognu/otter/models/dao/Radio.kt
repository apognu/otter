package com.github.apognu.otter.models.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.github.apognu.otter.models.api.FunkwhaleRadio

@Entity(tableName = "radios")
data class RadioEntity(
  @PrimaryKey
  val id: Int,
  var radio_type: String?,
  val name: String,
  val description: String,
  var related_object_id: String? = null
) {

  @androidx.room.Dao
  interface Dao {
    @Query("SELECT * FROM radios ORDER BY name")
    fun all(): LiveData<List<RadioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(radio: RadioEntity)
  }
}

fun FunkwhaleRadio.toDao(): RadioEntity = run {
  RadioEntity(id, radio_type, name, description, related_object_id)
}