# Add this to your $HOME/.zshrc
# autoload -Uz compinit && compinit
# source /path/to/this/folder/completion.zsh

_cp-stack() {
    local state context line
    typeset -A opt_args

    _arguments -C \
        '1: :->command' \
        '*: :->args'

    case $state in
        command)
            _values "cp-stack command" \
                "start[Start the stack]" \
                "stop[Stop the stack]" \
                "--help[Show help]" \
                "--version[Show version]"
            ;;
        args)
            case ${words[1]} in
                start)
                    _arguments \
                        '--local-api[Use local API]' \
                        '--local-ui[Use local UI]' \
                        '--help[Show help]'
                    ;;
                stop)
                    _arguments \
                        '--clear-databases[Clear databases]' \
                        '--help[Show help]'
                    ;;
            esac
            ;;
    esac
}

compdef _cp-stack cp-stack