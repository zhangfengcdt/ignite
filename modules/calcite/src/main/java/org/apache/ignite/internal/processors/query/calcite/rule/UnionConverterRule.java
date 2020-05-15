/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.query.calcite.rule;

import java.util.List;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalUnion;
import org.apache.calcite.tools.RelBuilder;
import org.apache.ignite.internal.processors.query.calcite.rel.IgniteConvention;
import org.apache.ignite.internal.processors.query.calcite.rel.IgniteUnionAll;
import org.apache.ignite.internal.processors.query.calcite.util.Commons;

/**
 *
 */
public class UnionConverterRule extends RelOptRule {
    /** */
    public static final RelOptRule INSTANCE = new UnionConverterRule();

    /** */
    public UnionConverterRule() {
        super(operand(LogicalUnion.class, any()));
    }

    /** {@inheritDoc} */
    @Override public void onMatch(RelOptRuleCall call) {
        LogicalUnion rel = call.rel(0);

        RelOptCluster cluster = rel.getCluster();
        List<RelNode> inputs = Commons.transform(rel.getInputs(),
            rel0 -> convert(rel0, IgniteConvention.INSTANCE));
        RelTraitSet traits = rel.getTraitSet()
            .replace(IgniteConvention.INSTANCE);

        RelNode res = new IgniteUnionAll(cluster, traits, inputs);

        if (!rel.all) {
            RelBuilder b = relBuilderFactory.create(rel.getCluster(), null);

            res = b.push(res).aggregate(b.groupKey(b.fields())).build();
        }

        RuleUtils.transformTo(call, res);
    }
}