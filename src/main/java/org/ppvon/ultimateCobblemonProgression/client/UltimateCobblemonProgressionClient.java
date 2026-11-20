package org.ppvon.ultimateCobblemonProgression.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.ppvon.ultimateCobblemonProgression.client.card.TrainerCardScreen;
import org.ppvon.ultimateCobblemonProgression.client.gym.RematchScreen;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardPayload;
import org.ppvon.ultimateCobblemonProgression.common.gym.RematchPayloads;
import net.fabricmc.loader.api.FabricLoader;

public class UltimateCobblemonProgressionClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // The server works out the Trainer Card; show it in its own screen.
        ClientPlayNetworking.registerGlobalReceiver(TrainerCardPayload.TYPE, (payload, context) ->
                context.client().setScreen(new TrainerCardScreen(payload.data())));
        // A Gym Challenge block's rematch menu.
        ClientPlayNetworking.registerGlobalReceiver(RematchPayloads.Open.TYPE, (payload, context) ->
                context.client().setScreen(new RematchScreen(payload.gym(), payload.entries())));

        boolean isLoaded = FabricLoader.getInstance().isModLoaded("rad-gyms");
        /*
        if(FabricLoader.getInstance().isModLoaded("rad-gyms")){
            RadGymsClientCompat.register();
        }
         */
    }
}
