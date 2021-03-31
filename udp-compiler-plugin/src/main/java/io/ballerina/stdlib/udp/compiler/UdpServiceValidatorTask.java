/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.udp.compiler;

import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import org.ballerinalang.stdlib.udp.Constants;

import java.util.List;
import java.util.Optional;

/**
 * Class to filter UDP services.
 */
public class UdpServiceValidatorTask implements AnalysisTask<SyntaxNodeAnalysisContext> {

    @Override
    public void perform(SyntaxNodeAnalysisContext ctx) {
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) ctx.node();
        String modulePrefix = getPrefix(ctx);

        Optional<Symbol> serviceDeclarationSymbol = ctx.semanticModel().symbol(serviceDeclarationNode);
        UdpServiceValidator udpServiceValidator = null;
        if (serviceDeclarationSymbol.isPresent()) {
            List<TypeSymbol> listenerTypes = ((ServiceDeclarationSymbol) serviceDeclarationSymbol.get())
                    .listenerTypes();
            for (TypeSymbol listenerType : listenerTypes) {
                if (listenerType.typeKind() == TypeDescKind.UNION
                        && ((UnionTypeSymbol) listenerType).memberTypeDescriptors().get(0).getModule()
                        .flatMap(Symbol::getName).orElse("").compareTo(Constants.UDP) == 0) {
                    udpServiceValidator = new UdpServiceValidator(ctx, modulePrefix
                            + SyntaxKind.COLON_TOKEN.stringValue());
                    break;
                } else if (listenerType.typeKind() == TypeDescKind.TYPE_REFERENCE
                        && ((TypeReferenceTypeSymbol) listenerType).typeDescriptor().getModule()
                        .flatMap(Symbol::getName).orElse("").compareTo(Constants.UDP) == 0) {
                    udpServiceValidator = new UdpServiceValidator(ctx, modulePrefix
                            + SyntaxKind.COLON_TOKEN.stringValue());
                    break;
                }
            }
        }

        if (udpServiceValidator != null) {
            udpServiceValidator.validate();
        }
    }

    private String getPrefix(SyntaxNodeAnalysisContext ctx) {
        ModulePartNode modulePartNode = ctx.syntaxTree().rootNode();
        for (ImportDeclarationNode importDeclaration : modulePartNode.imports()) {
            if (importDeclaration.moduleName().get(0).toString().split(" ")[0].compareTo(Constants.UDP) == 0) {
                if (importDeclaration.prefix().isPresent()) {
                    return importDeclaration.prefix().get().children().get(1).toString();
                }
                break;
            }
        }
        return Constants.UDP;
    }
}
