/*
 *  (c) 2013 Michael A. Beck, disco | Distributed Computer Systems Lab
 *                                  University of Kaiserslautern, Germany
 *         All Rights Reserved.
 *
 *  This software is work in progress and is released in the hope that it will
 *  be useful to the scientific community. It is provided "as is" without
 *  express or implied warranty, including but not limited to the correctness
 *  of the code or its suitability for any particular purpose.
 *
 *  You are free to use this software for any non-commercial educational or
 *  research purpose, provided that this copyright notice is not removed or
 *  modified. For commercial uses please contact the respective author(s).
 *
 *  If you find our software useful, we would appreciate if you mentioned it
 *  in any publication arising from the use of this software or acknowledge
 *  our work otherwise. We would also like to hear of any fixes or useful
 *  extensions to this software.
 *
 */

package unikl.disco.calculator.optimization;

import java.util.HashMap;
import java.util.Map;
import unikl.disco.calculator.symbolic_math.Arrival;
import unikl.disco.calculator.symbolic_math.Hoelder;
import unikl.disco.calculator.symbolic_math.ParameterMismatchException;
import unikl.disco.calculator.symbolic_math.ServerOverloadException;
import unikl.disco.calculator.symbolic_math.ThetaOutOfBoundException;
import unikl.disco.calculator.network.AbstractAnalysis;
import unikl.disco.calculator.network.Network;
import unikl.disco.calculator.network.AbstractAnalysis.Boundtype;

/**
 * This is a simple Gradient-search optimization. The search
 * starts with all Hoelder parameters being equal to 2 and a
 * theta equal to the thetagranularity. It then checks all 
 * bounds, which can be obtained by deviating from this point 
 * by "one step" - meaning either a single Hoelder parameter 
 * is changed by the hoeldergranularity or theta is changed 
 * by the thetagranularity.
 * We move then to the position, which delivered the best of
 * these values and repeat the process. If no neighbour can
 * deliver a better result than the current bound, we will 
 * not move and give the current bound as result instead.
 * @author Michael Beck
 * @author Sebastian Henningsen
 *
 */
public class SimpleGradient extends AbstractOptimizer {

	enum Change{
		THETA_DEC, THETA_INC, HOELDER_P, HOELDER_Q, NOTHING
	};

    /**
     * Creates an instance of this class, delegates the construction to @link AbstractOptimizer.
     * @param bound
     * @param boundtype
     */
    public SimpleGradient(Optimizable bound, AbstractAnalysis.Boundtype boundtype) {
		super(bound, boundtype);
	}
        
        @Override
        public double minimize(double thetagranularity, double hoeldergranularity) throws ThetaOutOfBoundException, ParameterMismatchException, ServerOverloadException {
            bound.prepare();
            // Initilializes the list of Hoelder-Parameters
            Map<Integer, Hoelder> allparameters = bound.getHoelderParameters();
            
            for(Map.Entry<Integer, Hoelder> entry : allparameters.entrySet()) {
                entry.getValue().setPValue(2);
            }
            
            // Initializes parameters
            maxTheta = bound.getMaximumTheta();
            //Debugging
            //System.out.println("Max Theta: " + maxTheta);
            double theta = thetagranularity;
            int changedHoelder = Integer.MAX_VALUE;
            boolean improved = true;
            Change change = SimpleGradient.Change.NOTHING;
            
            // Compute initial value
            double optValue;
            double newOptValue;
            try {
		optValue = bound.evaluate(theta);
            } catch(ServerOverloadException e) {
                optValue = Double.POSITIVE_INFINITY;
            }
            while(improved) {
                improved = false;
                change = SimpleGradient.Change.NOTHING;
                // Check if decreasing theta leads to a better result
                if(theta > thetagranularity) {
                    theta = theta - thetagranularity;
                    try {
                        newOptValue = bound.evaluate(theta);
                    } catch(ServerOverloadException | ThetaOutOfBoundException e) {
                        newOptValue = Double.POSITIVE_INFINITY;
                    }
                    if(optValue > newOptValue) {
                            optValue = newOptValue;
                            change = SimpleGradient.Change.THETA_DEC;
                    }

                    // Reset the changes
                    theta = theta + thetagranularity;
                }

                // Check if increasing theta leads to a better result
                if(theta < this.maxTheta - thetagranularity){
                    theta = theta + thetagranularity;
                    try {
                        newOptValue = bound.evaluate(theta);
                    } catch(             ServerOverloadException | ThetaOutOfBoundException e) {
                        newOptValue = Double.POSITIVE_INFINITY;
                    }
                    if(optValue > newOptValue) {
                        optValue = newOptValue;
                        change = SimpleGradient.Change.THETA_INC;
                    }
                    
                    // Reset changes again
                    theta = theta - thetagranularity;
                }
                // TODO: Get rid of the "call by reference" to the parameters, which is used here.
                // Check each neighbors resulting from decreasing the P-Value of Hoelder parameters
                for(Map.Entry<Integer, Hoelder> entry : allparameters.entrySet()){
                    double old_p_value = entry.getValue().getPValue();
                    if(entry.getValue().getPValue() < 2) {
                        entry.getValue().setPValue(-hoeldergranularity + entry.getValue().getPValue());
                    } else {
                        entry.getValue().setQValue(hoeldergranularity + entry.getValue().getQValue());
                    }
                    try {
                        newOptValue = bound.evaluate(theta);
                    } catch(ServerOverloadException | ThetaOutOfBoundException e){
                        newOptValue = Double.POSITIVE_INFINITY;
                    }
                    if(optValue > newOptValue) {
                        optValue = newOptValue; 
                        changedHoelder = entry.getKey();
                        change = SimpleGradient.Change.HOELDER_P;
                    }

                    // Reset changes
                    entry.getValue().setPValue(old_p_value);
                }

                // Check each neighbor by decreasing the Q-Value of Hoelder parameters
                for(Map.Entry<Integer, Hoelder> entry : allparameters.entrySet()){
                    double old_q_value = entry.getValue().getQValue();
                    if(entry.getValue().getPValue() < 2) {
                        entry.getValue().setPValue(hoeldergranularity + entry.getValue().getPValue());
                    } else {
                        entry.getValue().setQValue(-hoeldergranularity + entry.getValue().getQValue());
                    }
                    entry.getValue().setQValue(-hoeldergranularity + entry.getValue().getQValue());
                    try {
                        newOptValue = bound.evaluate(theta);
                    } catch(             ServerOverloadException | ThetaOutOfBoundException e) {
                        newOptValue = Double.POSITIVE_INFINITY;
                    }
                    if(optValue > newOptValue){
                            optValue = newOptValue; 
                            changedHoelder = entry.getKey();
                            change = SimpleGradient.Change.HOELDER_Q;
                    }

                    entry.getValue().setQValue(old_q_value);
                }

                switch(change) {
                    case THETA_INC:
                        theta = theta + thetagranularity;
                        improved = true;
                        break;
                    case THETA_DEC:
                        theta = theta - thetagranularity;
                        improved = true;
                        break;
                    case HOELDER_P:
                        allparameters.get(changedHoelder).setPValue(allparameters.get(changedHoelder).getPValue() - hoeldergranularity);
                        improved = true;
                        break;
                    case HOELDER_Q:
                        allparameters.get(changedHoelder).setQValue(allparameters.get(changedHoelder).getQValue() - hoeldergranularity);
                        improved = true;
                        break;
                    case NOTHING:
                        improved = false;
                        break;
                    default:
                        improved = false;
                        break;
                }
        }
        //System.out.println("Theta: "+theta+" Hoelder: "+allparameters.toString()+" Bound: "+optValue);

        return optValue; 
        }
        
	@Override
	public double Bound(Arrival input, Boundtype boundtype, double bound, double thetagranularity, double hoeldergranularity)
			throws ThetaOutOfBoundException, ParameterMismatchException, ServerOverloadException {
		
		double result;
		
		//Initializes the list of Hoelder-Parameters...
		Map<Integer, Hoelder> allparameters = new HashMap<>(0);
		allparameters.putAll(input.getSigma().getParameters());
		allparameters.putAll(input.getRho().getParameters());
			
		//If needed, the parameter, which represents the backlog, must be separated from the other Hoelder parameters
		if(boundtype == AbstractAnalysis.Boundtype.BACKLOG){
                    allparameters.get(allparameters.size()).setPValue(bound);
                    allparameters.remove(allparameters.size());
		}
		for(Map.Entry<Integer, Hoelder> entry : allparameters.entrySet()){
			entry.getValue().setPValue(2);
		}
		
		//Initializes theta
		double max_theta = input.getThetastar();
		double sigmapart;
		double rhopart;
		double theta = thetagranularity;
		int changed_hoelder = Integer.MAX_VALUE;
		boolean improved = true;
		
		Change change = SimpleGradient.Change.NOTHING;
		
		switch(boundtype){
			case BACKLOG:
				
				//Computes initial value
				double backlogprob;
				double new_backlogprob;
				try{
					backlogprob = input.evaluate(theta, 0, 0);
				}
				catch(ServerOverloadException e){
					backlogprob = Double.POSITIVE_INFINITY;
				}
				while(improved){
					improved = false;
					change = SimpleGradient.Change.NOTHING;
					//Check if decreasing theta leads to a better result
					if(theta > thetagranularity){
						
						theta = theta - thetagranularity;
						try{
							new_backlogprob = input.evaluate(theta, 0, 0);
						}
						catch(ServerOverloadException e){
							new_backlogprob = Double.POSITIVE_INFINITY;
						}
						catch(ThetaOutOfBoundException e){
							new_backlogprob = Double.POSITIVE_INFINITY;
						}
						if(backlogprob > new_backlogprob){
							backlogprob = new_backlogprob;
							change = SimpleGradient.Change.THETA_DEC;
						}
						
						theta = theta + thetagranularity;
					}
					
					//Check if increasing theta leads to a better result
					if(theta < max_theta - thetagranularity){
						theta = theta + thetagranularity;
						try{
							new_backlogprob = input.evaluate(theta, 0 , 0);
						}
						catch(ServerOverloadException e){
							new_backlogprob = Double.POSITIVE_INFINITY;
						}
						catch(ThetaOutOfBoundException e){
							new_backlogprob = Double.POSITIVE_INFINITY;
						}
						if(backlogprob > new_backlogprob){
							backlogprob = new_backlogprob;
							change = SimpleGradient.Change.THETA_INC;
						}
						
						theta = theta - thetagranularity;
					}
					
					//Check each neighbors resulting from decreasing the P-Value of Hoelder parameters
					for(Map.Entry<Integer, Hoelder> entry : allparameters.entrySet()){
						double old_p_value = entry.getValue().getPValue();
						if(entry.getValue().getPValue() < 2){
							entry.getValue().setPValue(-hoeldergranularity + entry.getValue().getPValue());
						}
						else{
							entry.getValue().setQValue(hoeldergranularity + entry.getValue().getQValue());
						}
						try{
							new_backlogprob = input.evaluate(theta, 0, 0);
						}
						catch(ServerOverloadException e){
							new_backlogprob = Double.POSITIVE_INFINITY;
						}
						catch(ThetaOutOfBoundException e){
							new_backlogprob = Double.POSITIVE_INFINITY;
						}
						if(backlogprob > new_backlogprob){
							backlogprob = new_backlogprob; 
							changed_hoelder = entry.getKey();
							change = SimpleGradient.Change.HOELDER_P;
						}
						
						entry.getValue().setPValue(old_p_value);
					}
					
					//Check each neighbor by decreasing the Q-Value of Hoelder parameters
					for(Map.Entry<Integer, Hoelder> entry : allparameters.entrySet()){
						double old_q_value = entry.getValue().getQValue();
						if(entry.getValue().getPValue() < 2){
							entry.getValue().setPValue(hoeldergranularity + entry.getValue().getPValue());
						}
						else{
							entry.getValue().setQValue(-hoeldergranularity + entry.getValue().getQValue());
						}
						entry.getValue().setQValue(-hoeldergranularity + entry.getValue().getQValue());
						try{
							new_backlogprob = input.evaluate(theta, 0 , 0);
						}
						catch(ServerOverloadException e){
							new_backlogprob = Double.POSITIVE_INFINITY;
						}
						catch(ThetaOutOfBoundException e){
							new_backlogprob = Double.POSITIVE_INFINITY;
						}
						if(backlogprob > new_backlogprob){
							backlogprob = new_backlogprob; 
							changed_hoelder = entry.getKey();
							change = SimpleGradient.Change.HOELDER_Q;
						}
						
						entry.getValue().setQValue(old_q_value);
					}
					
					switch(change){
					case THETA_INC:
						theta = theta + thetagranularity;
						improved = true;
						break;
					case THETA_DEC:
						theta = theta - thetagranularity;
						improved = true;
						break;
					case HOELDER_P:
						allparameters.get(changed_hoelder).setPValue(allparameters.get(changed_hoelder).getPValue() - hoeldergranularity);
						improved = true;
						break;
					case HOELDER_Q:
						allparameters.get(changed_hoelder).setQValue(allparameters.get(changed_hoelder).getQValue() - hoeldergranularity);
						improved = true;
						break;
					case NOTHING:
						improved = false;
						break;
					default:
						improved = false;
						break;
					}
					//System.out.println("Theta: "+theta+" Hoelder: "+allparameters.toString()+" Bound: "+backlogprob);
				}
				
				result = backlogprob;
				
				break;
				
			case DELAY:
				
				//Computes initial value
                                int delay = (int)Math.round(Math.ceil(bound));
				double delayprob;
				double new_delayprob;
				try{
					delayprob = input.evaluate(theta, delay, 0);
				}
				catch(ServerOverloadException e){
					delayprob = Double.POSITIVE_INFINITY;
				}
					
				while(improved){
					improved = false;
					change = SimpleGradient.Change.NOTHING;
					//Check if decreasing theta leads to a better result
					if(theta > thetagranularity){
						theta = theta - thetagranularity;
						try{
                                                    new_delayprob = input.evaluate(theta, delay, 0);
						}
						catch(ServerOverloadException e){
							new_delayprob = Double.POSITIVE_INFINITY;
						}
						if(delayprob > new_delayprob){
							delayprob = new_delayprob;
							change = SimpleGradient.Change.THETA_DEC;
						}
						theta = theta + thetagranularity;
					}
					
					//Check if increasing theta leads to a better result
					if(theta < max_theta - thetagranularity){
						theta = theta + thetagranularity;
						try{
							new_delayprob = input.evaluate(theta, delay, 0);
						}
						catch(ServerOverloadException e){
							new_delayprob = Double.POSITIVE_INFINITY;
						}
						if(delayprob > new_delayprob){
							delayprob = new_delayprob;
							change = SimpleGradient.Change.THETA_INC;
						}
						theta = theta - thetagranularity;
					}
					
					//Check each neighbor by decreasing the P-Value of Hoelder parameters
					for(Map.Entry<Integer, Hoelder> entry : allparameters.entrySet()){
						double old_p_value = entry.getValue().getPValue();
						if(entry.getValue().getPValue() < 2){
							entry.getValue().setPValue(-hoeldergranularity + entry.getValue().getPValue());
						}
						else{
							entry.getValue().setQValue(hoeldergranularity + entry.getValue().getQValue());
						}
						try{
							new_delayprob = input.evaluate(theta, delay, 0);
						}
						catch(ServerOverloadException e){
							new_delayprob = Double.POSITIVE_INFINITY;
						}
						catch(ThetaOutOfBoundException e){
							new_delayprob = Double.POSITIVE_INFINITY;
						}
						if(delayprob > new_delayprob){
							delayprob = new_delayprob;
							changed_hoelder = entry.getKey();
							change = SimpleGradient.Change.HOELDER_P;
						}
						entry.getValue().setPValue(old_p_value);
					}
					
					//Check each neighbor by decreasing the Q-Value of Hoelder parameters
					for(Map.Entry<Integer, Hoelder> entry : allparameters.entrySet()){
						double old_q_value = entry.getValue().getQValue();
						if(entry.getValue().getPValue() < 2){
							entry.getValue().setPValue(hoeldergranularity + entry.getValue().getPValue());
						}
						else{
							entry.getValue().setQValue(-hoeldergranularity + entry.getValue().getQValue());
						}
						try{
							new_delayprob = input.evaluate(theta, delay, 0);
						}
						catch(ServerOverloadException e){
							new_delayprob = Double.POSITIVE_INFINITY;
						}
						catch(ThetaOutOfBoundException e){
							new_delayprob = Double.POSITIVE_INFINITY;
						}
						if(delayprob > new_delayprob){
							delayprob = new_delayprob;
							changed_hoelder = entry.getKey();
							change = SimpleGradient.Change.HOELDER_Q;
						}
						entry.getValue().setQValue(old_q_value);
					}
					
					switch(change){
					case THETA_INC:
						theta = theta + thetagranularity;
						improved = true;
						break;
					case THETA_DEC:
						theta = theta - thetagranularity;
						improved = true;
						break;
					case HOELDER_P:
						allparameters.get(changed_hoelder).setPValue(allparameters.get(changed_hoelder).getPValue() + hoeldergranularity);
						improved = true;
						break;
					case HOELDER_Q:
						allparameters.get(changed_hoelder).setQValue(allparameters.get(changed_hoelder).getQValue() + hoeldergranularity);
						improved = true;
						break;
					case NOTHING:
						improved = false;
						break;
					default:
						improved = false;
						break;
					}
					//System.out.println("Theta: "+theta+" Hoelder: "+allparameters.toString()+" Bound: "+delayprob);
				}
				
				result = delayprob;
				
				break;
			case OUTPUT:
				//In case of an output-bound no result is needed
				result = Double.NaN;
				break;
			default:
				result = 0;
				break;
		}
		
		return result;
	}

	@Override
	public double ReverseBound(Arrival input, Boundtype boundtype, double violation_probability, double thetagranularity, double hoeldergranularity) throws ThetaOutOfBoundException, ParameterMismatchException, ServerOverloadException {

		double result;
		
		//Initializes the list of Hoelder-Parameters...
		HashMap<Integer, Hoelder> allparameters = new HashMap<Integer, Hoelder>(0);
		allparameters.putAll(input.getSigma().getParameters());
		allparameters.putAll(input.getRho().getParameters());
			
		//If needed, the parameter, which represents the backlog, must be separated from the other Hoelder parameters
		if(boundtype == AbstractAnalysis.Boundtype.BACKLOG){
			allparameters.get(allparameters.size()).setPValue(0);
			allparameters.remove(allparameters.size());
		}
		//debugging
		//System.out.println("allparameters:"+ allparameters.toString());
		for(Map.Entry<Integer, Hoelder> entry : allparameters.entrySet()){
			entry.getValue().setPValue(2);
		}
		
		//Initializes theta
		double max_theta = input.getThetastar();
		double sigmapart;
		double rhopart;
		double theta = thetagranularity;
		int changed_hoelder = Integer.MAX_VALUE;
		boolean improved = true;
		
		Change change = SimpleGradient.Change.NOTHING;
		
		switch(boundtype){
			case BACKLOG:
				
				//Computes initial value
				double backlogvalue;
				double new_backlog;
				try{
					sigmapart = 1/theta*Math.log(input.evaluate(theta, 0, 0));
					backlogvalue = -Math.log(violation_probability)/theta + sigmapart;
				}
				catch(ServerOverloadException e){
					backlogvalue = Double.POSITIVE_INFINITY;
				}
				while(improved){
					improved = false;
					change = SimpleGradient.Change.NOTHING;
					//Check if decreasing theta leads to a better result
					if(theta > thetagranularity){
						
						theta = theta - thetagranularity;
						try{
							sigmapart = 1/theta*Math.log(input.evaluate(theta, 0, 0));
							new_backlog = -Math.log(violation_probability)/theta + sigmapart;
						}
						catch(ServerOverloadException e){
							new_backlog = Double.POSITIVE_INFINITY;
						}
						catch(ThetaOutOfBoundException e){
							new_backlog = Double.POSITIVE_INFINITY;
						}
						if(backlogvalue > new_backlog){
							backlogvalue = new_backlog;
							change = SimpleGradient.Change.THETA_DEC;
						}
						
						theta = theta + thetagranularity;
					}
					
					//Check if increasing theta leads to a better result
					if(theta < max_theta - thetagranularity){
						theta = theta + thetagranularity;
						try{
							sigmapart = 1/theta*Math.log(input.evaluate(theta, 0, 0));
							new_backlog = -Math.log(violation_probability)/theta + sigmapart;
						}
						catch(ServerOverloadException e){
							new_backlog = Double.POSITIVE_INFINITY;
						}
						catch(ThetaOutOfBoundException e){
							new_backlog = Double.POSITIVE_INFINITY;
						}
						if(backlogvalue > new_backlog){
							backlogvalue = new_backlog;
							change = SimpleGradient.Change.THETA_INC;
						}
						
						theta = theta - thetagranularity;
					}
					
					//Check each neighbors resulting from decreasing the P-Value of Hoelder parameters
					for(Map.Entry<Integer, Hoelder> entry : allparameters.entrySet()){
						double old_p_value = entry.getValue().getPValue();
						if(entry.getValue().getPValue() < 2){
							entry.getValue().setPValue(-hoeldergranularity + entry.getValue().getPValue());
						}
						else{
							entry.getValue().setQValue(hoeldergranularity + entry.getValue().getQValue());
						}
						try{
							sigmapart = 1/theta*Math.log(input.evaluate(theta, 0, 0));
							new_backlog = -Math.log(violation_probability)/theta + sigmapart;
						}
						catch(ServerOverloadException e){
							new_backlog = Double.POSITIVE_INFINITY;
						}
						catch(ThetaOutOfBoundException e){
							new_backlog = Double.POSITIVE_INFINITY;
						}
						if(backlogvalue > new_backlog){
							backlogvalue = new_backlog; 
							changed_hoelder = entry.getKey();
							change = SimpleGradient.Change.HOELDER_P;
						}
						
						entry.getValue().setPValue(old_p_value);
					}
					
					//Check each neighbor by decreasing the Q-Value of Hoelder parameters
					for(Map.Entry<Integer, Hoelder> entry : allparameters.entrySet()){
						double old_q_value = entry.getValue().getQValue();
						if(entry.getValue().getPValue() < 2){
							entry.getValue().setPValue(hoeldergranularity + entry.getValue().getPValue());
						}
						else{
							entry.getValue().setQValue(-hoeldergranularity + entry.getValue().getQValue());
						}
						entry.getValue().setQValue(-hoeldergranularity + entry.getValue().getQValue());
						try{
							sigmapart = 1/theta*Math.log(input.evaluate(theta, 0, 0));
							new_backlog = -Math.log(violation_probability)/theta + sigmapart;
						}
						catch(ServerOverloadException e){
							new_backlog = Double.POSITIVE_INFINITY;
						}
						catch(ThetaOutOfBoundException e){
							new_backlog = Double.POSITIVE_INFINITY;
						}
						if(backlogvalue > new_backlog){
							backlogvalue = new_backlog; 
							changed_hoelder = entry.getKey();
							change = SimpleGradient.Change.HOELDER_Q;
						}
						
						entry.getValue().setQValue(old_q_value);
					}
					
					switch(change){
					case THETA_INC:
						theta = theta + thetagranularity;
						improved = true;
						break;
					case THETA_DEC:
						theta = theta - thetagranularity;
						improved = true;
						break;
					case HOELDER_P:
						allparameters.get(changed_hoelder).setPValue(allparameters.get(changed_hoelder).getPValue() - hoeldergranularity);
						improved = true;
						break;
					case HOELDER_Q:
						allparameters.get(changed_hoelder).setQValue(allparameters.get(changed_hoelder).getQValue() - hoeldergranularity);
						improved = true;
						break;
					case NOTHING:
						improved = false;
						break;
					default:
						improved = false;
						break;
					}
					//System.out.println("Theta: "+theta+" Hoelder: "+allparameters.toString()+" Bound: "+backlogvalue);
				}
				
				result = backlogvalue;
				
				break;
				
			case DELAY:
				
				//Computes initial value
				double delayvalue;
				double new_delay;
				try{
					sigmapart = input.getSigma().getValue(theta, input.getSigma().getParameters());
					rhopart = input.getRho().getValue(theta, input.getRho().getParameters());
					delayvalue = -1/rhopart*(-Math.log(violation_probability)/theta + sigmapart);
				}
				catch(ServerOverloadException e){
					delayvalue = Double.POSITIVE_INFINITY;
				}
					
				while(improved){
					improved = false;
					change = SimpleGradient.Change.NOTHING;
					//Check if decreasing theta leads to a better result
					if(theta > thetagranularity){
						theta = theta - thetagranularity;
						try{
							sigmapart = input.getSigma().getValue(theta, input.getSigma().getParameters());
							rhopart = input.getRho().getValue(theta, input.getRho().getParameters());
							new_delay = -1/rhopart*(-Math.log(violation_probability)/theta + sigmapart);
						}
						catch(ServerOverloadException e){
							new_delay = Double.POSITIVE_INFINITY;
						}
						if(delayvalue > new_delay){
							delayvalue = new_delay;
							change = SimpleGradient.Change.THETA_DEC;
						}
						theta = theta + thetagranularity;
					}
					
					//Check if increasing theta leads to a better result
					if(theta < max_theta - thetagranularity){
						theta = theta + thetagranularity;
						try{
							sigmapart = input.getSigma().getValue(theta, input.getSigma().getParameters());
							rhopart = input.getRho().getValue(theta, input.getRho().getParameters());
							new_delay = -1/rhopart*(-Math.log(violation_probability)/theta + sigmapart);
						}
						catch(ServerOverloadException e){
							new_delay = Double.POSITIVE_INFINITY;
						}
						if(delayvalue > new_delay){
							delayvalue = new_delay;
							change = SimpleGradient.Change.THETA_INC;
						}
						theta = theta - thetagranularity;
					}
					
					//Check each neighbor by decreasing the P-Value of Hoelder parameters
					for(Map.Entry<Integer, Hoelder> entry : allparameters.entrySet()){
						double old_p_value = entry.getValue().getPValue();
						if(entry.getValue().getPValue() < 2){
							entry.getValue().setPValue(-hoeldergranularity + entry.getValue().getPValue());
						}
						else{
							entry.getValue().setQValue(hoeldergranularity + entry.getValue().getQValue());
						}
						try{
							sigmapart = input.getSigma().getValue(theta, input.getSigma().getParameters());
							rhopart = input.getRho().getValue(theta, input.getRho().getParameters());
							new_delay = -1/rhopart*(-Math.log(violation_probability)/theta + sigmapart);
						}
						catch(ServerOverloadException e){
							new_delay = Double.POSITIVE_INFINITY;
						}
						catch(ThetaOutOfBoundException e){
							new_delay = Double.POSITIVE_INFINITY;
						}
						if(delayvalue > new_delay){
							delayvalue = new_delay;
							changed_hoelder = entry.getKey();
							change = SimpleGradient.Change.HOELDER_P;
						}
						entry.getValue().setPValue(old_p_value);
					}
					
					//Check each neighbor by decreasing the Q-Value of Hoelder parameters
					for(Map.Entry<Integer, Hoelder> entry : allparameters.entrySet()){
						double old_q_value = entry.getValue().getQValue();
						if(entry.getValue().getPValue() < 2){
							entry.getValue().setPValue(hoeldergranularity + entry.getValue().getPValue());
						}
						else{
							entry.getValue().setQValue(-hoeldergranularity + entry.getValue().getQValue());
						}
						try{
							sigmapart = input.getSigma().getValue(theta, input.getSigma().getParameters());
							rhopart = input.getRho().getValue(theta, input.getRho().getParameters());
							new_delay = -1/rhopart*(-Math.log(violation_probability)/theta + sigmapart);
						}
						catch(ServerOverloadException e){
							new_delay = Double.POSITIVE_INFINITY;
						}
						catch(ThetaOutOfBoundException e){
							new_delay = Double.POSITIVE_INFINITY;
						}
						if(delayvalue > new_delay){
							delayvalue = new_delay;
							changed_hoelder = entry.getKey();
							change = SimpleGradient.Change.HOELDER_Q;
						}
						entry.getValue().setQValue(old_q_value);
					}
					
					switch(change){
					case THETA_INC:
						theta = theta + thetagranularity;
						improved = true;
						break;
					case THETA_DEC:
						theta = theta - thetagranularity;
						improved = true;
						break;
					case HOELDER_P:
						allparameters.get(changed_hoelder).setPValue(allparameters.get(changed_hoelder).getPValue() + hoeldergranularity);
						improved = true;
						break;
					case HOELDER_Q:
						allparameters.get(changed_hoelder).setQValue(allparameters.get(changed_hoelder).getQValue() + hoeldergranularity);
						improved = true;
						break;
					case NOTHING:
						improved = false;
						break;
					default:
						improved = false;
						break;
					}
					//System.out.println("Theta: "+theta+" Hoelder: "+allparameters.toString()+" Bound: "+delayvalue);
				}
				
				result = delayvalue;
				
				break;
			case OUTPUT:
				//In case of an output-bound no result is needed
				result = Double.NaN;
				break;
			default:
				result = 0;
				break;
		}
		
		return result;
	}

}
