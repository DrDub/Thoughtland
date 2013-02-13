/*
 *   This file is part of Thoughtland -- Verbalizing n-dimensional objects.
 *   Copyright (C) 2013 Pablo Duboue <pablo.duboue@gmail.com>
 * 
 *   Thoughtland is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as 
 *   published by the Free Software Foundation, either version 3 of 
 *   the License, or (at your option) any later version.
 *
 *   Meetdle is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *   
 *   You should have received a copy of the GNU Affero General Public 
 *   License along with Thoughtland.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.duboue.thoughtland.cloud.weka;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.duboue.thoughtland.cloud.weka.WekaClassifierExtractor;

import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.functions.neural.NeuralConnection;
import weka.classifiers.functions.neural.NeuralNode;

public class MultilayerPerceptronExtractor extends WekaClassifierExtractor<MultilayerPerceptron> {

	private Field neuralNodes;

	public MultilayerPerceptronExtractor() {
		this.neuralNodes = super.pinpoint(MultilayerPerceptron.class, "m_neuralNodes");
	}

	@Override
	public double[] extract(MultilayerPerceptron clazz) {
		List<Double> weights = new ArrayList<Double>();

		NeuralConnection[] nodes = this.extract(clazz, this.neuralNodes);
		for (NeuralConnection node : nodes) {
			double[] ws = ((NeuralNode) node).getWeights();
			for (double w : ws)
				weights.add(w);
		}

		double[] result = new double[weights.size()];
		for (int i = 0; i < weights.size(); i++)
			result[i] = weights.get(i);
		return result;
	}

}
