package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;

import java.util.Random;

public class RepartidorAgent extends Agent {

    protected void setup() {

        System.out.println(
                getLocalName() +
                " iniciado."
        );

        registrarServicio();

        addBehaviour(new CyclicBehaviour() {

            @Override
            public void action() {

                ACLMessage msg = receive();

                if (msg != null) {

                    System.out.println(
                            getLocalName() +
                            " recibió orden."
                    );

                    /*
                     Lógica de negocio:
                     calcular tiempo entrega
                     */

                    Random r = new Random();

                    int tiempoEntrega =
                            5 + r.nextInt(10);

                    ACLMessage reply =
                            msg.createReply();

                    reply.setPerformative(
                            ACLMessage.INFORM
                    );

                    reply.setContent(
                            "Entrega en " +
                            tiempoEntrega +
                            " minutos"
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

            sd.setType("repartidor");
            sd.setName("delivery");

            dfd.addServices(sd);

            DFService.register(this, dfd);

        } catch (FIPAException e) {

            e.printStackTrace();

        }

    }
}