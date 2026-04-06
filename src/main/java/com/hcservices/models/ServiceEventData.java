package com.hcservices.models;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import javax.annotation.Nullable;

public class ServiceEventData {

    public static final BuilderCodec<ServiceEventData> CODEC = BuilderCodec.builder(ServiceEventData.class, ServiceEventData::new)
        .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action).add()
        .append(new KeyedCodec<>("ListingId", Codec.STRING), (d, v) -> d.listingId = v, d -> d.listingId).add()
        .append(new KeyedCodec<>("ContractId", Codec.STRING), (d, v) -> d.contractId = v, d -> d.contractId).add()
        .append(new KeyedCodec<>("Filter", Codec.STRING), (d, v) -> d.filter = v, d -> d.filter).add()
        .append(new KeyedCodec<>("@Title", Codec.STRING), (d, v) -> d.title = v, d -> d.title).add()
        .append(new KeyedCodec<>("@Description", Codec.STRING), (d, v) -> d.description = v, d -> d.description).add()
        .append(new KeyedCodec<>("@Price", Codec.STRING), (d, v) -> d.price = v, d -> d.price).add()
        .append(new KeyedCodec<>("@ClientNotes", Codec.STRING), (d, v) -> d.clientNotes = v, d -> d.clientNotes).add()
        .build();

    @Nullable public String action;
    @Nullable public String listingId;
    @Nullable public String contractId;
    @Nullable public String filter;
    @Nullable public String title;
    @Nullable public String description;
    @Nullable public String price;
    @Nullable public String clientNotes;

    public ServiceEventData() {}
}
