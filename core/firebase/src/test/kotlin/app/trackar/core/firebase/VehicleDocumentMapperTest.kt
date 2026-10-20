package app.trackar.core.firebase

import app.trackar.core.model.DeviceStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VehicleDocumentMapperTest {

    private val fixTime = Timestamp(1_767_268_800, 0)

    @Test
    fun `full document is mapped`() {
        val vehicle = vehicleFromDocument("42", document())!!

        assertEquals("42", vehicle.id)
        assertEquals("Hilux", vehicle.name)
        assertEquals(DeviceStatus.ONLINE, vehicle.status)
        val position = vehicle.position!!
        assertEquals(-23.55, position.coordinates.latitude, 0.0)
        assertEquals(60.0, position.speedKmh, 0.0)
        assertEquals(true, position.ignition)
        assertEquals(1_767_268_800_000, position.fixTime.toEpochMilli())
    }

    @Test
    fun `document without name is skipped`() {
        assertNull(vehicleFromDocument("1", document() - VehicleFields.NAME))
    }

    @Test
    fun `position without location is dropped but vehicle is kept`() {
        val data = document() + (VehicleFields.POSITION to mapOf(VehicleFields.FIX_TIME to fixTime))
        val vehicle = vehicleFromDocument("1", data)!!
        assertNull(vehicle.position)
    }

    @Test
    fun `missing unique id falls back to document id`() {
        val vehicle = vehicleFromDocument("abc", document() - VehicleFields.UNIQUE_ID)!!
        assertEquals("abc", vehicle.uniqueId)
    }

    @Test
    fun `status is mapped case-insensitively`() {
        assertEquals(DeviceStatus.OFFLINE, "OFFLINE".toDeviceStatus())
        assertEquals(DeviceStatus.UNKNOWN, null.toDeviceStatus())
    }

    private fun document(): Map<String, Any?> = mapOf(
        VehicleFields.NAME to "Hilux",
        VehicleFields.UNIQUE_ID to "IMEI-1",
        VehicleFields.STATUS to "online",
        VehicleFields.LAST_UPDATE to fixTime,
        VehicleFields.POSITION to mapOf(
            VehicleFields.LOCATION to GeoPoint(-23.55, -46.63),
            VehicleFields.FIX_TIME to fixTime,
            VehicleFields.SPEED_KMH to 60L,
            VehicleFields.IGNITION to true
        )
    )
}
