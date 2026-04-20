package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;

import java.util.Random;

public class RestauranteAgent extends Agent {

    protected void setup() {

        System.out.println(getLocalName() + " iniciado.");

        registrarServicio();

        addBehaviour(new CyclicBehaviour() {

            @Override
            public void action() {

                ACLMessage msg = receive();

                if (msg != null) {

                    System.out.println(
                            getLocalName() +
                            " recibió solicitud."
                    );

                    /*
                     Lógica de negocio:
                     calcular precio y tiempo
                     */

                    Random r = new Random();

                    int precio =
                            10 + r.nextInt(10);

                    int tiempo =
                            15 + r.nextInt(10);

                    ACLMessage reply =
                            msg.createReply();

                    reply.setPerformative(
                            ACLMessage.PROPOSE
                    );

                    reply.setContent(
                            "Precio: " +
                            precio +
                            ", Tiempo: " +
                            tiempo
                    );

                    send(reply);

                } else {

                    block();

                }

            }
        });
    }

    private void registrarServicio() {

        try {

            DFAgentDescription dfd =
                    new DFAgentDescription();

            dfd.setName(getAID());

            ServiceDescription sd =
                    new ServiceDescription();

            sd.setType("restaurante");
            sd.setName("venta-comida");

            dfd.addServices(sd);

            DFService.register(this, dfd);

        } catch (FIPAException e) {

            e.printStackTrace();

        }

    }
}