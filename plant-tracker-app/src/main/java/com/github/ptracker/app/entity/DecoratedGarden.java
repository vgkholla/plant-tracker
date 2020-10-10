package com.github.ptracker.app.entity;

import com.apollographql.apollo.ApolloClient;
import com.gitgub.ptracker.app.GetGardenQuery;
import com.github.ptracker.app.util.ApolloClientCallback;
import com.github.ptracker.entity.Garden;
import com.github.ptracker.entity.GardenPlant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.github.ptracker.app.entity.VerifierUtils.*;
import static com.google.common.base.Preconditions.*;


public class DecoratedGarden {
  private final ApolloClient _graphQLClient;
  private final String _id;
  private final DecoratedAccount _parentAccount;

  private Garden _garden;
  private List<GardenPlant> _displayGardenPlants;
  private List<DecoratedGardenPlant> _gardenPlants;

  DecoratedGarden(ApolloClient graphQLClient, String id, DecoratedAccount account) {
    _graphQLClient = checkNotNull(graphQLClient, "graphQLClient cannot be null");
    _id = verifyStringFieldNotNullOrEmpty(id, "Garden ID cannot be empty");
    _parentAccount = checkNotNull(account, "Account cannot be null");
  }

  public String getId() {
    return _id;
  }

  public DecoratedAccount getParentAccount() {
    return _parentAccount;
  }

  public Garden getGarden() {
    populate();
    return _garden;
  }

  public List<GardenPlant> getDisplayGardenPlants() {
    populate();
    return _displayGardenPlants;
  }

  public List<DecoratedGardenPlant> getGardenPlants() {
    populate();
    return _gardenPlants;
  }

  @Override
  public String toString() {
    return "DecoratedGarden{" + "_gardenId='" + _id + '\'' + ", _parentAccount=" + _parentAccount + '}';
  }

  private void populate() {
    if (_garden == null) {
      synchronized (this) {
        if (_garden == null) {
          ApolloClientCallback<GetGardenQuery.Data, GetGardenQuery.GetGarden> callback =
              new ApolloClientCallback<>(GetGardenQuery.Data::getGarden);
          _graphQLClient.query(new GetGardenQuery(_id)).enqueue(callback);
          GetGardenQuery.GetGarden getGarden = callback.getNonNullOrThrow(10, TimeUnit.SECONDS);
          if (getGarden.id() == null) {
            throw new IllegalStateException("Could not find garden with ID: " + _id);
          }
          _garden =
              Garden.newBuilder().setId(_id).setName(getGarden.name()).setAccountId(_parentAccount.getId()).build();
          _displayGardenPlants = getGarden.gardenPlants() == null ? Collections.emptyList() : getGarden.gardenPlants()
              .stream()
              .map(gardenPlant -> GardenPlant.newBuilder()
                  .setId(gardenPlant.id())
                  .setName(gardenPlant.name())
                  .setGardenId(_id)
                  .build())
              .collect(Collectors.toList());
          _gardenPlants = _displayGardenPlants.stream()
              .map(gardenPlant -> new DecoratedGardenPlant(_graphQLClient, gardenPlant.getId(), this))
              .collect(Collectors.toList());
        }
      }
    }
  }
}