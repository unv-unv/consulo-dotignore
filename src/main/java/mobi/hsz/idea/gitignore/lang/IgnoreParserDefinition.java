/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 hsz Jakub Chrzanowski <jakub@hsz.mobi>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package mobi.hsz.idea.gitignore.lang;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IFileElementType;
import consulo.language.ast.TokenSet;
import consulo.language.ast.TokenType;
import consulo.language.file.FileViewProvider;
import consulo.language.lexer.Lexer;
import consulo.language.parser.ParserDefinition;
import consulo.language.parser.PsiParser;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.version.LanguageVersion;
import jakarta.annotation.Nonnull;
import mobi.hsz.idea.gitignore.file.type.IgnoreFileType;
import mobi.hsz.idea.gitignore.lexer.IgnoreLexerAdapter;
import mobi.hsz.idea.gitignore.parser.IgnoreParser;
import mobi.hsz.idea.gitignore.psi.IgnoreFile;
import mobi.hsz.idea.gitignore.psi.IgnoreTypes;

/**
 * Defines the implementation of a parser for a custom language.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.1
 */
public class IgnoreParserDefinition implements ParserDefinition {
    /** Whitespaces. */
    public static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);

    /** Regular comment started with # */
    public static final TokenSet COMMENTS = TokenSet.create(IgnoreTypes.COMMENT);

    /** Section comment started with ## */
    public static final TokenSet SECTIONS = TokenSet.create(IgnoreTypes.SECTION);

    /** Header comment started with ### */
    public static final TokenSet HEADERS = TokenSet.create(IgnoreTypes.HEADER);

    /** Negation element - ! in the beginning of the entry */
    public static final TokenSet NEGATIONS = TokenSet.create(IgnoreTypes.NEGATION);

    /** Brackets [] */
    public static final TokenSet BRACKETS = TokenSet.create(IgnoreTypes.BRACKET_LEFT, IgnoreTypes.BRACKET_RIGHT);

    /** Slashes / */
    public static final TokenSet SLASHES = TokenSet.create(IgnoreTypes.SLASH);

    /** Syntax syntax: */
    public static final TokenSet SYNTAXES = TokenSet.create(IgnoreTypes.SYNTAX_KEY);

    /** All values - parts of paths */
    public static final TokenSet VALUES = TokenSet.create(IgnoreTypes.VALUE);

    /** Element type of the node describing a file in the specified language. */
    public static final IFileElementType FILE = new IFileElementType(Language.findInstance(IgnoreLanguage.class));

    private final Language language;

    public IgnoreParserDefinition(Language language) {
        this.language = language;
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return language;
    }

    /**
     * Returns the lexer for lexing files in the specified project. This lexer does not need to support incremental
     * relexing - it is always called for the entire file.
     *
     * @return the lexer instance.
     */
    @Nonnull
    @Override
    public Lexer createLexer(@Nonnull LanguageVersion languageVersion) {
        return new IgnoreLexerAdapter();
    }

    /**
     * Returns the parser for parsing files in the specified project.
     *
     * @return the parser instance.
     */
    @Nonnull
    @Override
    public PsiParser createParser(@Nonnull LanguageVersion languageVersion) {
        return new IgnoreParser();
    }

    /**
     * Returns the element type of the node describing a file in the specified language.
     *
     * @return the file node element type.
     */
    @Nonnull
    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    /**
     * Returns the set of token types which are treated as whitespace by the PSI builder. Tokens of those types are
     * automatically skipped by PsiBuilder. Whitespace elements on the bounds of nodes built by PsiBuilder are
     * automatically excluded from the text range of the nodes. <p><strong>It is strongly advised you return TokenSet
     * that only contains {@link TokenType#WHITE_SPACE}, which is suitable for all the languages unless
     * you really need to use special whitespace token</strong>
     *
     * @return the set of whitespace token types.
     */
    @Nonnull
    @Override
    public TokenSet getWhitespaceTokens(@Nonnull LanguageVersion languageVersion) {
        return WHITE_SPACES;
    }

    /**
     * Returns the set of token types which are treated as comments by the PSI builder.
     * Tokens of those types are automatically skipped by PsiBuilder. Also, To Do patterns
     * are searched in the text of tokens of those types.
     *
     * @return the set of comment token types.
     */
    @Nonnull
    @Override
    public TokenSet getCommentTokens(@Nonnull LanguageVersion languageVersion) {
        return COMMENTS;
    }

    /**
     * Returns the set of element types which are treated as string literals. "Search in strings"
     * option in refactorings is applied to the contents of such tokens.
     *
     * @return the set of string literal element types.
     */
    @Nonnull
    @Override
    public TokenSet getStringLiteralElements(@Nonnull LanguageVersion languageVersion) {
        return TokenSet.EMPTY;
    }

    /**
     * Creates a PSI element for the specified AST node. The AST tree is a simple, semantic-free
     * tree of AST nodes which is built during the PsiBuilder parsing pass. The PSI tree is built
     * over the AST tree and includes elements of different types for different language constructs.
     *
     * @param node the node for which the PSI element should be returned.
     * @return the PSI element matching the element type of the AST node.
     */
    @Nonnull
    @Override
    @RequiredReadAction
    public PsiElement createElement(@Nonnull ASTNode node) {
        return IgnoreTypes.Factory.createElement(node);
    }

    /**
     * Creates a PSI element for the specified virtual file.
     *
     * @param viewProvider virtual file.
     * @return the PSI file element.
     */
    @Nonnull
    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return viewProvider.getBaseLanguage() instanceof IgnoreLanguage ignoreLanguage
            ? ignoreLanguage.createFile(viewProvider)
            : new IgnoreFile(viewProvider, IgnoreFileType.INSTANCE);
    }
}
