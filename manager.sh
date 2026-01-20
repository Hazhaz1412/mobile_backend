#!/usr/bin/env bash
set -e

# ========= CONFIG =========
COMPOSE_CMD="podman-compose"

# ========= UI =========
title() {
    clear 
}

pause() {
    read -p "Enter..." _
}
 

compose_down() {
    echo ">> podman-compose down"
    $COMPOSE_CMD down
}

compose_up() {
    echo ">> podman-compose up -d"
    $COMPOSE_CMD up -d
}

list_containers() {
    echo ">> podman ps"
    podman ps
}

list_images() {
    echo ">> podman images"
    podman images
}
 
logs_by_container() {
    mapfile -t containers < <(podman ps --format "{{.Names}}")

    if [ ${#containers[@]} -eq 0 ]; then
        echo "x no containers"
        return
    fi

    echo "container: "
    select c in "${containers[@]}"; do
        if [ -n "$c" ]; then
            echo ">> podman logs $c"
            podman logs "$c"
            break
        else
            echo "x"
        fi
    done
}
 
logs_by_service() {
    services=$($COMPOSE_CMD config --services)

    if [ -z "$services" ]; then
        echo "x"
        return
    fi

    echo "service: "
    select svc in $services; do
        if [ -n "$svc" ]; then
            echo ">> podman-compose logs $svc"
            $COMPOSE_CMD logs "$svc"
            break
        else
            echo "x"
        fi
    done
}

restart_compose() {
    echo ">> podman-compose restart"
    $COMPOSE_CMD restart
}
 
menu() {
    title
    echo "1) podman-compose down"
    echo "2) podman-compose up -d"
    echo "3) podman ps "
    echo "4) podman images"
    echo "5) logs container"
    echo "6) logs service"
    echo "7) podman-compose restart"
    echo "0) exit"
    echo "-------------------------------------"
    echo -n "cmd...: "
}
 
while true; do
    menu
    read choice
    echo

    case $choice in
        1) compose_down ;;
        2) compose_up ;;
        3) list_containers ;;
        4) list_images ;;
        5) logs_by_container ;;
        6) logs_by_service ;;
        7) restart_compose ;;
        0) echo "Bye"; exit 0 ;;
        *) echo "X" ;;
    esac

    echo
    pause
done
