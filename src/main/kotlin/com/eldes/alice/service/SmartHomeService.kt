package com.eldes.alice.service

import com.eldes.alice.domain.GateTarget
import com.eldes.alice.model.SmartHomeActionDevice
import com.eldes.alice.model.SmartHomeActionRequest
import com.eldes.alice.model.SmartHomeActionResponse
import com.eldes.alice.model.SmartHomeActionResponsePayload
import com.eldes.alice.model.SmartHomeActionResult
import com.eldes.alice.model.SmartHomeActionResultDevice
import com.eldes.alice.model.SmartHomeActionResultState
import com.eldes.alice.model.SmartHomeCapabilityActionResult
import com.eldes.alice.model.SmartHomeCapabilityCurrentState
import com.eldes.alice.model.SmartHomeCapabilityDescription
import com.eldes.alice.model.SmartHomeCurrentState
import com.eldes.alice.model.SmartHomeDevice
import com.eldes.alice.model.SmartHomeDevicesPayload
import com.eldes.alice.model.SmartHomeDevicesRequest
import com.eldes.alice.model.SmartHomeDevicesResponse
import com.eldes.alice.model.SmartHomeQueryDevice
import com.eldes.alice.model.SmartHomeQueryPayload
import com.eldes.alice.model.SmartHomeQueryResponse
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.slf4j.LoggerFactory

class SmartHomeService(
    private val eldesApiClient: EldesApiClient,
) {
    private val logger = LoggerFactory.getLogger(SmartHomeService::class.java)

    suspend fun devices(requestId: String, bearerToken: String): SmartHomeDevicesResponse {
        val devices = eldesApiClient.getDevices(bearerToken)
        val availableDeviceIds = devices.zones.flatMap { it.devices }.map { it.id }.toSet()
        val smartDevices = smartHomeDevices.filter { it.target.deviceId in availableDeviceIds }
            .map { it.toResponseDevice() }

        return SmartHomeDevicesResponse(
            requestId = requestId,
            payload = SmartHomeDevicesPayload(
                userId = devices.userId,
                devices = smartDevices,
            ),
        )
    }

    fun query(requestId: String, request: SmartHomeDevicesRequest): SmartHomeQueryResponse =
        SmartHomeQueryResponse(
            requestId = requestId,
            payload = SmartHomeQueryPayload(
                devices = request.devices.map { device ->
                    SmartHomeQueryDevice(
                        id = device.id,
                        capabilities = listOf(
                            SmartHomeCapabilityCurrentState(
                                state = SmartHomeCurrentState(value = false),
                            )
                        ),
                    )
                },
            ),
        )

    suspend fun action(
        requestId: String,
        bearerToken: String,
        request: SmartHomeActionRequest,
    ): SmartHomeActionResponse =
        SmartHomeActionResponse(
            requestId = requestId,
            payload = SmartHomeActionResponsePayload(
                devices = request.payload.devices.map { device ->
                    handleActionDevice(bearerToken, device)
                },
            ),
        )

    private suspend fun handleActionDevice(
        bearerToken: String,
        device: SmartHomeActionDevice,
    ): SmartHomeActionResultDevice {
        val smartDevice = smartHomeDevices.firstOrNull { it.id == device.id }
            ?: return deviceError(device.id, "DEVICE_NOT_FOUND", "Устройство не найдено")

        val onOff = device.capabilities.firstOrNull { it.type == "devices.capabilities.on_off" }
            ?: return deviceError(device.id, "INVALID_ACTION", "Поддерживается только действие открытия")

        if (onOff.state.instance != "on") {
            return capabilityResult(device.id, "ERROR", "INVALID_ACTION", "Устройство поддерживает только открытие")
        }
        if (onOff.state.value != true) {
            return capabilityResult(device.id, "DONE")
        }

        return runCatching {
            eldesApiClient.open(smartDevice.target, bearerToken)
        }.fold(
            onSuccess = {
                capabilityResult(device.id, "DONE")
            },
            onFailure = { error ->
                logger.error("Smart Home failed to open ${smartDevice.target.key}", error)
                capabilityResult(device.id, "ERROR", "DEVICE_UNREACHABLE", "Не удалось открыть устройство")
            },
        )
    }

    private fun capabilityResult(
        deviceId: String,
        status: String,
        errorCode: String? = null,
        errorMessage: String? = null,
    ): SmartHomeActionResultDevice =
        SmartHomeActionResultDevice(
            id = deviceId,
            capabilities = listOf(
                SmartHomeCapabilityActionResult(
                    state = SmartHomeActionResultState(
                        actionResult = SmartHomeActionResult(
                            status = status,
                            errorCode = errorCode,
                            errorMessage = errorMessage,
                        ),
                    ),
                )
            ),
        )

    private fun deviceError(deviceId: String, code: String, message: String): SmartHomeActionResultDevice =
        SmartHomeActionResultDevice(
            id = deviceId,
            actionResult = SmartHomeActionResult(
                status = "ERROR",
                errorCode = code,
                errorMessage = message,
            ),
        )

    private data class SmartHomeGateDevice(
        val id: String,
        val name: String,
        val description: String,
        val room: String,
        val target: GateTarget,
    ) {
        fun toResponseDevice(): SmartHomeDevice =
            SmartHomeDevice(
                id = id,
                name = name,
                description = description,
                room = room,
                capabilities = listOf(SmartHomeCapabilityDescription()),
                customData = JsonObject(mapOf("target" to JsonPrimitive(target.key))),
            )
    }

    private companion object {
        val smartHomeDevices = listOf(
            SmartHomeGateDevice("yard_in", "Шлагбаум", "Въезд во двор", "Двор", GateTarget.byKey("yard_in")!!),
            SmartHomeGateDevice("yard_out", "Шлагбаум выезд", "Выезд из двора", "Двор", GateTarget.byKey("yard_out")!!),
            SmartHomeGateDevice("parking_in", "Ворота паркинга въезд", "Въезд в паркинг", "Паркинг", GateTarget.byKey("parking_in")!!),
            SmartHomeGateDevice("parking_out", "Ворота паркинга выезд", "Выезд из паркинга", "Паркинг", GateTarget.byKey("parking_out")!!),
        )
    }
}
