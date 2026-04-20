package agents;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;

public class ClienteAgent extends Agent {

    protected void setup() {

        System.out.println(getLocalName() + " iniciado.");

        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {

                try {

                    /*
                     Buscar el servicio "gestor"
                     en las páginas amarillas
                     */

                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("gestor");

                    template.addServices(sd);

                    DFAgentDescription[] result =
                            DFService.search(myAgent, template);

                    if (result.length > 0) {

                        /*
                         Enviar solicitud de pedido
                         */

                        ACLMessage msg =
                                new ACLMessage(ACLMessage.REQUEST);

                        msg.addReceiver(result[0].getName());

                        msg.setContent("Pedido: Hamburguesa");

                        send(msg);

                        System.out.println(
                                getLocalName() +
                                " envió pedido al gestor."
                        );

                    } else {

                        System.out.println(
                                "No se encontró gestor."
                        );

                    }

                } catch (FIPAException e) {

                    e.printStackTrace();

                }

            }
        });
    }
}
