from datetime import date
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict

DocumentType = Literal["TRAVEL_TICKET", "HOTEL_BOOKING", "UNKNOWN"]


class TravelTicketFields(BaseModel):
    model_config = ConfigDict(extra="forbid")

    passenger: str | None = None
    origin: str | None = None
    destinationCountry: str | None = None
    destinationCityOrAirport: str | None = None
    departureDate: date | None = None
    returnOrArrivalDate: date | None = None


class HotelBookingFields(BaseModel):
    model_config = ConfigDict(extra="forbid")

    guest: str | None = None
    hotelCountry: str | None = None
    hotelCity: str | None = None
    checkInDate: date | None = None
    checkOutDate: date | None = None


class UnknownFields(BaseModel):
    model_config = ConfigDict(extra="forbid")


class ExtractionResponse(BaseModel):
    schemaVersion: str = "1"
    documentType: DocumentType
    extractedFields: TravelTicketFields | HotelBookingFields | UnknownFields
    extraction: dict[str, str]


def validate_fields(document_type: DocumentType, fields: dict[str, Any]) -> TravelTicketFields | HotelBookingFields | UnknownFields:
    if document_type == "TRAVEL_TICKET":
        return TravelTicketFields.model_validate(fields)
    if document_type == "HOTEL_BOOKING":
        return HotelBookingFields.model_validate(fields)
    return UnknownFields.model_validate(fields)
