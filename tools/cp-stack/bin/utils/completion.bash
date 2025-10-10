# Add this to your $HOME/.bashrc
# source /path/to/this/folder/completion.bash

_cp_stack_completion()
{
    local cur prev
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"

    case $prev in
        start)
            COMPREPLY=($(compgen -W "--local-api --local-ui --help" -- "$cur"))
            return
            ;;
        stop)
            COMPREPLY=($(compgen -W "--clear-databases --help" -- "$cur"))
            return
            ;;
        --local-api|--local-ui|--clear-databases|--help)
            COMPREPLY=()
            return
            ;;
    esac

    if [[ ${COMP_CWORD} -eq 1 ]]; then
        COMPREPLY=($(compgen -W "start stop --help --version" -- "$cur"))
    else
        local main_command="${COMP_WORDS[1]}"
        case $main_command in
            start)
                COMPREPLY=($(compgen -W "--local-api --local-ui --help" -- "$cur"))
                ;;
            stop)
                COMPREPLY=($(compgen -W "--clear-databases --help" -- "$cur"))
                ;;
            *)
                COMPREPLY=()
                ;;
        esac
    fi
}

complete -F _cp_stack_completion cp-stack