package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;

public class GestorAgent extends Agent {

    protected void setup() {

        System.out.println(getLocalName() + " iniciado.");

        /*
         Registrar servicio en páginas amarillas
         */

        try {

            DFAgentDescription dfd =
                    new DFAgentDescription();

            dfd.setName(getAID());

            ServiceDescription sd =
                    new ServiceDescription();

            sd.setType("gestor");
            sd.setName("gestion-pedidos");

            dfd.addServices(sd);

            DFService.register(this, dfd);

        } catch (FIPAException e) {

            e.printStackTrace();

        }

        /*
         Esperar mensajes
         */

        addBehaviour(new CyclicBehaviour() {

            @Override
            public void action() {

                ACLMessage msg = receive();

                if (msg != null) {

                    System.out.println(
                            getLocalName() +
                            " recibió: " +
                            msg.getContent()
                    );

                    buscarRestaurantes();

                } else {

                    block();

                }

            }
        });
    }

    private void buscarRestaurantes() {

        try {

            /*
             Buscar restaurantes en DF
             */

            DFAgentDescription template =
                    new DFAgentDescription();

            ServiceDescription sd =
                    new ServiceDescription();

            sd.setType("restaurante");

            template.addServices(sd);

            DFAgentDescription[] result =
                    DFService.search(this, template);

            System.out.println(
                    "Restaurantes encontrados: " +
                    result.length
            );

            for (int i = 0; i < result.length; i++) {

                ACLMessage cfp =
                        new ACLMessage(
                                ACLMessage.CFP
                        );

                cfp.addReceiver(
                        result[i].getName()
                );

                cfp.setContent(
                        "Solicitud de precio"
                );

                send(cfp);

            }

        } catch (FIPAException e) {

            e.printStackTrace();

        }

    }
}